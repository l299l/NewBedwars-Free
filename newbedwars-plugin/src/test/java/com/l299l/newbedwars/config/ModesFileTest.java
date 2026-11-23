package com.l299l.newbedwars.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModesFileTest {
    private static final Set<String> MODES = Set.of("Solo", "Doubles", "Squads", "Rush", "Duels");
    private static final Set<String> PHASES = Set.of("Solo", "Doubles", "Squads", "Rush", "Duels", "Default");

    @TempDir
    Path dir;

    private String bundledText;
    private Logger logger;
    private final List<LogRecord> records = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        bundledText = Files.readString(Path.of("src/main/resources/modes.yml"), StandardCharsets.UTF_8);
        logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new Handler() {
            public void publish(LogRecord r) {
                records.add(r);
            }

            public void flush() {
            }

            public void close() {
            }
        });
    }

    @AfterEach
    void tearDown() {
        ModesFile.use(null, null);
    }

    private Path file() {
        return dir.resolve("modes.yml");
    }

    private void load(Object phases, Object modes) {
        new ModesFile(dir, bundledText, logger).load(phases, modes);
    }

    private String text() throws IOException {
        return Files.readString(file(), StandardCharsets.UTF_8);
    }

    private boolean logged(Level level, String part) {
        return records.stream().anyMatch(r -> r.getLevel().equals(level) && r.getMessage().contains(part));
    }

    private static Set<String> modeKeys(Map<String, Object> root) {
        Set<String> out = new java.util.LinkedHashSet<>();
        for (Object k : ((Map<?, ?>) root.get("Modes")).keySet()) out.add(String.valueOf(k));
        return out;
    }

    private static List<String> phaseIds(Map<String, Object> root) {
        List<String> out = new ArrayList<>();
        for (Object o : (List<?>) root.get("GamePhases")) out.add(String.valueOf(((Map<?, ?>) o).get("ID")));
        return out;
    }

    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + 1)) n++;
        return n;
    }

    private void assertComplete() throws IOException {
        Map<String, Object> root = ModesFile.parse(text());
        assertNotNull(root);
        assertTrue(modeKeys(root).containsAll(MODES));
        assertTrue(phaseIds(root).containsAll(PHASES));
        assertEquals(phaseIds(root).size(), new java.util.HashSet<>(phaseIds(root)).size());
        assertEquals(root, ModesFile.current());
    }

    @Test
    void firstStartCopiesTheBundledFileByteForByte() throws IOException {
        load(null, null);
        assertEquals(bundledText, text());
        assertEquals(MODES, modeKeys(ModesFile.current()));
        assertEquals(PHASES, new java.util.HashSet<>(phaseIds(ModesFile.current())));
        assertEquals(MODES, modeKeys(ModesFile.bundled()));
        assertTrue(records.isEmpty());
    }

    @Test
    void legacyConfigValuesAreMigratedAndMissingBuiltInsAdded() throws IOException {
        Map<String, Object> customPhase = new LinkedHashMap<>();
        customPhase.put("ID", "Custom");
        customPhase.put("Phases", List.of(Map.of("Name", "Only", "Duration", 0)));
        Map<String, Object> oldRush = new LinkedHashMap<>();
        oldRush.put("ID", "Rush");
        oldRush.put("Phases", List.of(Map.of("Name", "OldRushPhase", "Duration", 0)));
        Map<String, Object> trios = new LinkedHashMap<>();
        trios.put("Generators", "Solo");
        trios.put("GamePhases", "Custom");
        trios.put("MaxInTeam", 3);
        trios.put("Teams", 6);
        Map<String, Object> legacyModes = new LinkedHashMap<>();
        legacyModes.put("Trios", trios);

        load(List.of(customPhase, oldRush), legacyModes);

        String written = text();
        assertTrue(written.startsWith(bundledText.substring(0, bundledText.indexOf("\nModes:") + 1)));
        Map<String, Object> root = ModesFile.parse(written);
        assertNotNull(root);
        assertTrue(modeKeys(root).contains("Trios"));
        assertTrue(modeKeys(root).containsAll(MODES));
        List<String> ids = phaseIds(root);
        assertTrue(ids.containsAll(PHASES));
        assertTrue(ids.contains("Custom"));
        assertEquals(1, ids.stream().filter("Rush"::equals).count());
        assertTrue(written.contains("OldRushPhase"));
        assertEquals(1, count(written, "OldRushPhase"));
        assertTrue(logged(Level.INFO, "moved"));
        assertFalse(logged(Level.WARNING, "no longer read"));
        assertEquals(root, ModesFile.current());
    }

    @Test
    void userCommentsAndCustomEntriesSurviveAndCompletionIsIdempotent() throws IOException {
        String user = """
                # my own header
                Modes:
                  # keep me
                  Solo:
                    Generators: Solo
                    GamePhases: Solo
                  Mine:
                    Generators: Squads
                    GamePhases: MyPhases
                    MaxInTeam: 3

                # phases below
                GamePhases:
                  - ID: MyPhases
                    Phases:
                      # inner comment
                      - Name: "Only"
                        Duration: 0
                  - ID: Solo
                    Phases:
                      - Name: "Mine solo"
                        Duration: 0
                """;
        Files.writeString(file(), user, StandardCharsets.UTF_8);

        load(null, null);

        String first = text();
        assertTrue(first.contains("# my own header\nModes:\n  # keep me\n  Solo:\n    Generators: Solo\n    GamePhases: Solo\n  Mine:\n    Generators: Squads\n    GamePhases: MyPhases\n    MaxInTeam: 3\n"));
        assertTrue(first.contains("# phases below\nGamePhases:\n  - ID: MyPhases\n    Phases:\n      # inner comment\n      - Name: \"Only\"\n        Duration: 0\n  - ID: Solo\n    Phases:\n      - Name: \"Mine solo\"\n        Duration: 0\n"));
        assertComplete();
        assertEquals(1, count(first, "  Solo:\n"));
        assertEquals(1, count(first, "  - ID: Solo\n"));
        assertEquals(1, count(first, "  Duels:\n"));
        assertEquals(1, count(first, "  - ID: Default\n"));
        assertTrue(logged(Level.INFO, "Added the built-in"));

        records.clear();
        load(null, null);
        assertEquals(first, text());
        assertTrue(records.isEmpty());
        assertComplete();
    }

    @Test
    void differentIndentationGetsShiftedInserts() throws IOException {
        String user = """
                GamePhases:
                - ID: X
                  Phases:
                  - Name: "a"
                    Duration: 0
                Modes:
                    Mine:
                        Generators: Solo
                        MaxInTeam: 2
                """;
        Files.writeString(file(), user, StandardCharsets.UTF_8);

        load(null, null);

        assertComplete();
        String written = text();
        assertTrue(written.contains("\n- ID: Solo\n  Phases:\n"));
        assertTrue(written.contains("\n    Solo:\n      Generators: Solo\n"));
        assertTrue(modeKeys(ModesFile.current()).contains("Mine"));
        assertTrue(phaseIds(ModesFile.current()).contains("X"));
        assertFalse(logged(Level.WARNING, "Could not add"));
        Map<?, ?> solo = (Map<?, ?>) ((Map<?, ?>) ModesFile.current().get("Modes")).get("Solo");
        assertEquals(1, solo.get("MaxInTeam"));
    }

    @Test
    void emptySectionsAreFilled() throws IOException {
        Files.writeString(file(), "Modes: {}\nGamePhases: []\n", StandardCharsets.UTF_8);

        load(null, null);

        assertComplete();
        String written = text();
        assertFalse(written.contains("{}"));
        assertFalse(written.contains("[]\n  - ID") || written.startsWith("GamePhases: []"));
        assertEquals(MODES, modeKeys(ModesFile.current()));
        assertEquals(PHASES, new java.util.HashSet<>(phaseIds(ModesFile.current())));
    }

    @Test
    void missingGamePhasesKeyGetsASectionAppended() throws IOException {
        String user = "Modes:\n  Solo:\n    Generators: Solo\n    GamePhases: Solo\n";
        Files.writeString(file(), user, StandardCharsets.UTF_8);

        load(null, null);

        assertComplete();
        String written = text();
        assertTrue(written.startsWith(user.substring(0, user.length() - 1)));
        assertEquals(1, count(written, "\nGamePhases:\n"));
        assertEquals(1, count(written, "  Solo:\n"));
    }

    @Test
    void missingModesKeyGetsASectionAppended() throws IOException {
        String user = "GamePhases:\n  - ID: Custom\n    Phases:\n      - Name: \"x\"\n        Duration: 0\n";
        Files.writeString(file(), user, StandardCharsets.UTF_8);

        load(null, null);

        assertComplete();
        assertEquals(1, count(text(), "\nModes:\n"));
    }

    @Test
    void invalidYamlLeavesTheFileUntouchedAndUsesBuiltIns() throws IOException {
        String broken = "Modes:\n  Solo: [unclosed\n\tGamePhases: : :\n";
        Files.writeString(file(), broken, StandardCharsets.UTF_8);

        load(null, null);

        assertEquals(broken, text());
        assertTrue(logged(Level.SEVERE, "not valid YAML"));
        assertTrue(ModesFile.current().isEmpty());
        assertEquals(MODES, modeKeys(ModesFile.bundled()));
        assertEquals(PHASES, new java.util.HashSet<>(phaseIds(ModesFile.bundled())));
    }

    @Test
    void legacyValuesWhileTheFileExistsOnlyWarn() throws IOException {
        String user = "Modes:\n  Mine:\n    Generators: Solo\nGamePhases:\n  - ID: MyPhases\n    Phases:\n      - Name: \"x\"\n        Duration: 0\n";
        Files.writeString(file(), user, StandardCharsets.UTF_8);
        Map<String, Object> legacyPhase = Map.of("ID", "LegacyOnly", "Phases", List.of(Map.of("Name", "L", "Duration", 0)));

        load(List.of(legacyPhase), Map.of("LegacyMode", Map.of("Generators", "Solo")));

        assertTrue(logged(Level.WARNING, "no longer read"));
        String written = text();
        assertFalse(written.contains("LegacyOnly"));
        assertFalse(written.contains("LegacyMode"));
        assertTrue(written.contains("Mine:"));
        assertTrue(modeKeys(ModesFile.current()).contains("Mine"));
        assertFalse(modeKeys(ModesFile.current()).contains("LegacyMode"));
        assertComplete();
    }

    private static List<com.l299l.newbedwars.api.mode.PhaseInfo> onePhase() {
        return List.of(new com.l299l.newbedwars.api.mode.PhaseInfo("Go \"now\"", 30, List.of("DIAMOND_GENERATOR:2")),
                new com.l299l.newbedwars.api.mode.PhaseInfo("End", 0, List.of()));
    }

    @Test
    void newModeIsAppendedAndOtherCommentsStay() {
        String edited = ModesFile.withMode(bundledText, "Fun", ModesFile.modeBlock("Fun", "Solo", "Default", null, "upgradeSolo", 3, 6));
        Map<String, Object> root = ModesFile.parse(edited);
        assertNotNull(root);
        assertTrue(ModesFile.hasMode(root, "Fun"));
        Map<?, ?> fun = (Map<?, ?>) ((Map<?, ?>) root.get("Modes")).get("Fun");
        assertEquals("Solo", fun.get("Generators"));
        assertEquals(3, fun.get("MaxInTeam"));
        assertFalse(fun.containsKey("ShopGui"));
        assertTrue(modeKeys(root).containsAll(MODES));
        assertEquals(phaseIds(ModesFile.parse(bundledText)), phaseIds(root));
        assertTrue(edited.contains("#   Generators  - generator config ID"));
    }

    @Test
    void replacingAModeChangesOnlyItsBlock() {
        String edited = ModesFile.withMode(bundledText, "Squads", ModesFile.modeBlock("Squads", "Duels", null, null, null, 2, 0));
        Map<String, Object> before = ModesFile.parse(bundledText);
        Map<String, Object> root = ModesFile.parse(edited);
        Map<?, ?> squads = (Map<?, ?>) ((Map<?, ?>) root.get("Modes")).get("Squads");
        assertEquals(Map.of("Generators", "Duels", "MaxInTeam", 2), squads);
        for (String other : List.of("Solo", "Doubles", "Rush", "Duels")) {
            assertEquals(((Map<?, ?>) before.get("Modes")).get(other), ((Map<?, ?>) root.get("Modes")).get(other));
        }
        assertEquals(before.get("GamePhases"), root.get("GamePhases"));
        assertTrue(edited.contains("#   Generators  - generator config ID"));
    }

    @Test
    void modeWithoutSettingsStillLoadsAsASection() {
        Map<String, Object> root = ModesFile.parse(ModesFile.withMode(bundledText, "Empty", ModesFile.modeBlock("Empty", null, " ", null, null, 0, 0)));
        assertTrue(ModesFile.hasMode(root, "Empty"));
    }

    @Test
    void modeIsRemovedAndMissingOneReturnsNull() {
        String edited = ModesFile.withoutMode(bundledText, "Rush");
        Map<String, Object> root = ModesFile.parse(edited);
        assertFalse(ModesFile.hasMode(root, "Rush"));
        assertTrue(ModesFile.hasMode(root, "Duels"));
        assertTrue(ModesFile.hasMode(root, "Squads"));
        assertNull(ModesFile.withoutMode(bundledText, "Nope"));
    }

    @Test
    void phaseListsAreAddedReplacedAndRemoved() {
        String added = ModesFile.withPhaseList(bundledText, "Quick", ModesFile.phaseBlock("Quick", onePhase()));
        Map<String, Object> root = ModesFile.parse(added);
        assertTrue(ModesFile.hasPhaseList(root, "Quick"));
        List<?> lists = (List<?>) root.get("GamePhases");
        Map<?, ?> quick = (Map<?, ?>) lists.get(lists.size() - 1);
        Map<?, ?> first = (Map<?, ?>) ((List<?>) quick.get("Phases")).get(0);
        assertEquals("Go \"now\"", first.get("Name"));
        assertEquals(30, first.get("Duration"));
        assertEquals(List.of("DIAMOND_GENERATOR:2"), first.get("Actions"));

        String replaced = ModesFile.withPhaseList(added, "Solo", ModesFile.phaseBlock("Solo", onePhase()));
        Map<String, Object> after = ModesFile.parse(replaced);
        assertEquals(phaseIds(root), phaseIds(after));
        Map<?, ?> solo = (Map<?, ?>) ((List<?>) after.get("GamePhases")).get(0);
        assertEquals(2, ((List<?>) solo.get("Phases")).size());
        assertTrue(replaced.contains("  - ID: Doubles"));

        String removed = ModesFile.withoutPhaseList(replaced, "Quick");
        assertFalse(ModesFile.hasPhaseList(ModesFile.parse(removed), "Quick"));
        assertTrue(ModesFile.hasPhaseList(ModesFile.parse(removed), "Default"));
        assertNull(ModesFile.withoutPhaseList(bundledText, "Nope"));
    }

    @Test
    void editWritesTheFileAndRefusesAnUnexpectedResult() throws IOException {
        load(null, null);
        ModesFile modes = new ModesFile(dir, "", logger);
        Map<String, Object> root = modes.edit(t -> ModesFile.withMode(t, "Fun", ModesFile.modeBlock("Fun", null, null, null, null, 2, 0)),
                r -> ModesFile.hasMode(r, "Fun"));
        assertTrue(ModesFile.hasMode(root, "Fun"));
        assertTrue(text().contains("  Fun:\n    MaxInTeam: 2"));
        String before = text();
        assertThrows(IOException.class, () -> modes.edit(t -> ModesFile.withoutMode(t, "Fun"), r -> ModesFile.hasMode(r, "Fun")));
        assertThrows(IOException.class, () -> modes.edit(t -> null, r -> true));
        assertThrows(IOException.class, () -> modes.edit(t -> "Modes: [", r -> true));
        assertEquals(before, text());
    }

    @Test
    void modeBlockKeepsTheTablistKey() {
        List<String> block = ModesFile.modeBlock("Fun", null, null, null, null, 2, 0, "Fancy");
        assertTrue(block.contains("    Tablist: \"Fancy\""));
        Map<String, Object> root = ModesFile.parse(ModesFile.withMode(bundledText, "Fun", block));
        assertEquals("Fancy", ((Map<?, ?>) ((Map<?, ?>) root.get("Modes")).get("Fun")).get("Tablist"));
        assertFalse(ModesFile.modeBlock("Fun", null, null, null, null, 2, 0).toString().contains("Tablist"));
    }
}
