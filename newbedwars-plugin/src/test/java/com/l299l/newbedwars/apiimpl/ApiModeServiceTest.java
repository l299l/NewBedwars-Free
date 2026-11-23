package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.mode.EditResult;
import com.l299l.newbedwars.api.mode.ModeInfo;
import com.l299l.newbedwars.api.mode.ModeResult;
import com.l299l.newbedwars.api.mode.PhaseListInfo;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.phases.Phase;
import com.l299l.newbedwars.arena.phases.PhaseAction;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.ModesFile;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.config.properties.GameMode;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUIManager;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiModeServiceTest {
    private Map<String, GameMode> savedModes;
    private HashMap<String, GamePhases> savedPhases;
    private HashMap<String, GeneratorLeveling> levelings;
    private IArena arena;
    private NewBedwars plugin;
    private GUIManager guis;

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path dir;

    @BeforeEach
    void setUp() {
        plugin = mock(NewBedwars.class);
        levelings = new HashMap<>();
        when(plugin.getGeneratorLeveling()).thenReturn(levelings);
        guis = mock(GUIManager.class);
        when(plugin.getGuiManager()).thenReturn(guis);
        when(plugin.getDataFolder()).thenReturn(dir.toFile());
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        NewBedwars.plugin = plugin;
        savedModes = Properties.Modes;
        savedPhases = Properties.GamePhases;
        Properties.Modes = new LinkedHashMap<>();
        Properties.GamePhases = new HashMap<>();
        arena = mock(IArena.class);
        when(arena.getTeams()).thenReturn(Map.of("a", mock(Team.class), "b", mock(Team.class)));
        when(arena.status()).thenReturn(GameStatus.waiting);
        when(arena.getArenaName()).thenReturn("castle");
        ArenaRegistry.clear();
        ArenaRegistry.register("castle", arena);
    }

    @AfterEach
    void tearDown() {
        Properties.Modes = savedModes;
        Properties.GamePhases = savedPhases;
        ModesFile.use(Map.of(), Map.of());
        ArenaRegistry.clear();
        NewBedwars.plugin = null;
    }

    private void mode(String id, String generators, String phases, int size, int teams) {
        Properties.Modes.put(id, new GameMode(id, generators, phases, null, null, size, teams));
    }

    @Test
    void modesFlagBuiltInFromBundledFile() {
        ModesFile.use(Map.of(), Map.of(ModesFile.MODES, Map.of("Squads", Map.of())));
        mode("Squads", "fast", "quick", 4, 4);
        mode("Custom", null, null, 0, 0);
        List<ModeInfo> modes = ApiModeService.modes();
        assertEquals(2, modes.size());
        assertEquals(new ModeInfo("Squads", "fast", "quick", null, null, 4, 4, true), modes.get(0));
        assertFalse(modes.get(1).builtIn());
    }

    @Test
    void noBundledModesMeansNothingIsBuiltIn() {
        ModesFile.use(Map.of(), Map.of());
        mode("Squads", null, null, 0, 0);
        assertFalse(ApiModeService.modes().get(0).builtIn());
    }

    @Test
    void phaseListsAreSortedAndDescribePhases() {
        Properties.GamePhases.put("zeta", new GamePhases("zeta", List.of()));
        Phase one = new Phase("One", 60, List.of(new PhaseAction("A:1"), new PhaseAction("B")));
        Phase two = new Phase("Two", null, null);
        Properties.GamePhases.put("alpha", new GamePhases("alpha", List.of(one, two)));
        List<PhaseListInfo> lists = ApiModeService.phaseLists();
        assertEquals(List.of("alpha", "zeta"), lists.stream().map(PhaseListInfo::id).toList());
        var phases = lists.get(0).phases();
        assertEquals("One", phases.get(0).name());
        assertEquals(60, phases.get(0).duration());
        assertEquals(List.of("A:1", "B"), phases.get(0).actions());
        assertEquals(0, phases.get(1).duration());
        assertTrue(phases.get(1).actions().isEmpty());
        assertTrue(lists.get(1).phases().isEmpty());
    }

    @Test
    void unknownArenaOrModeIsNotFound() {
        mode("Free", null, null, 0, 0);
        assertEquals(ModeResult.Code.NOT_FOUND, ApiModeService.apply("nope", "Free").code());
        assertEquals(ModeResult.Code.NOT_FOUND, ApiModeService.apply(null, "Free").code());
        assertEquals(ModeResult.Code.NOT_FOUND, ApiModeService.apply("castle", "Ghost").code());
        assertEquals(ModeResult.Code.NOT_FOUND, ApiModeService.apply("castle", null).code());
    }

    @Test
    void runningArenaIsBusy() {
        mode("Free", null, null, 0, 0);
        when(arena.status()).thenReturn(GameStatus.playing);
        assertEquals(ModeResult.Code.BUSY, ApiModeService.apply("castle", "Free").code());
        verify(arena, never()).setMaxInTeam(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void missingPartsAreReported() {
        mode("Broken", "NoGen", null, 0, 0);
        ModeResult result = ApiModeService.apply("castle", "Broken");
        assertEquals(ModeResult.Code.MISSING_PARTS, result.code());
        assertEquals(List.of("NoGen"), result.missing());
        assertFalse(result.teamsDiffer());
    }

    @Test
    void okAppliesSavesAndReportsTeamMismatch() {
        mode("Duels", null, null, 3, 4);
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            ModeResult result = ApiModeService.apply("castle", "duels");
            assertEquals(ModeResult.Code.OK, result.code());
            assertTrue(result.teamsDiffer());
            verify(json.constructed().get(0)).save();
        }
        verify(arena).setMaxInTeam(3);
    }

    @Test
    void publicMethodRunsOnMainThread() throws Exception {
        mode("Free", null, null, 0, 0);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            CompletableFuture<ModeResult> future = new ApiModeService().applyMode("castle", "Free");
            assertEquals(ModeResult.Code.OK, future.get().code());
            assertEquals(1, new ApiModeService().getModes().get().size());
            levelings.put("b", mock(GeneratorLeveling.class));
            levelings.put("a", mock(GeneratorLeveling.class));
            assertEquals(List.of("a", "b"), new ApiModeService().getGeneratorConfigs().get());
        }
    }

    @Test
    void arenaModesListTheCurrentSettingsSortedByName() {
        when(arena.getGeneratorsLevelingId()).thenReturn("Solo");
        when(arena.getGamePhasesId()).thenReturn("Solo");
        when(arena.getShopGuiId()).thenReturn("exampleShop");
        when(arena.getUpgradeGuiId()).thenReturn("upgradeSolo");
        when(arena.getMaxInTeam()).thenReturn(1);
        IArena other = mock(IArena.class);
        when(other.getArenaName()).thenReturn("Aqua");
        when(other.status()).thenReturn(GameStatus.playing);
        when(other.getTeams()).thenReturn(Map.of());
        ArenaRegistry.register("Aqua", other);

        List<com.l299l.newbedwars.api.mode.ArenaModeInfo> list = ApiModeService.arenaModes();

        assertEquals(List.of("Aqua", "castle"), list.stream().map(com.l299l.newbedwars.api.mode.ArenaModeInfo::arena).toList());
        assertEquals(new com.l299l.newbedwars.api.mode.ArenaModeInfo("Aqua", false, null, null, null, null, 0, 0), list.get(0));
        assertEquals(new com.l299l.newbedwars.api.mode.ArenaModeInfo("castle", true, "Solo", "Solo", "exampleShop", "upgradeSolo", 1, 2), list.get(1));
    }

    private void loadBundled() throws Exception {
        String text = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/modes.yml"));
        java.nio.file.Files.writeString(dir.resolve("modes.yml"), text);
        Map<String, Object> root = new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new org.yaml.snakeyaml.LoaderOptions())).load(text);
        ModesFile.use(root, root);
        Properties.reloadModes();
    }

    private static EditResult run(ModeEdits.Plan plan) {
        return plan.runNow();
    }

    private static ModeInfo info(String id, String gen, String phases, int size, int teams) {
        return new ModeInfo(id, gen, phases, null, null, size, teams, false);
    }

    @Test
    void saveModeCreatesAndReplaces() throws Exception {
        loadBundled();
        levelings.put("Solo", mock(GeneratorLeveling.class));
        assertEquals(EditResult.Code.OK, run(ModeEdits.saveMode(info("Fun", "Solo", "Default", 3, 6))).code());
        assertEquals(new GameMode("Fun", "Solo", "Default", null, null, 3, 6), Properties.Modes.get("Fun"));
        assertTrue(java.nio.file.Files.readString(dir.resolve("modes.yml")).contains("#   Generators  - generator config ID"));

        assertEquals(EditResult.Code.OK, run(ModeEdits.saveMode(info("fUN", null, null, 0, 0))).code());
        assertEquals(1, Properties.Modes.keySet().stream().filter(k -> k.equalsIgnoreCase("fun")).count());
        assertEquals(new GameMode("Fun", null, null, null, null, 0, 0), Properties.Modes.get("Fun"));

        assertEquals(EditResult.Code.OK, run(ModeEdits.saveMode(info("Squads", "Solo", null, 5, 4))).code());
        assertEquals(5, Properties.Modes.get("Squads").maxInTeam());
        assertTrue(Properties.Modes.containsKey("Duels"));
    }

    @Test
    void saveModeRejectsBadInput() throws Exception {
        loadBundled();
        when(guis.getGui("shop")).thenReturn(mock(com.l299l.newbedwars.gui.GUISave.class));
        for (String id : new String[]{null, "", "a b", "x".repeat(33), "true", "-", "a:b"}) {
            assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveMode(info(id, null, null, 0, 0))).code(), String.valueOf(id));
        }
        EditResult result = run(ModeEdits.saveMode(new ModeInfo("Bad", "NoGen", "NoPhases", "shop", "noUpgrade", 101, -1, false)));
        assertEquals(EditResult.Code.INVALID, result.code());
        assertEquals(5, result.problems().size());
        assertTrue(result.problems().contains("unknown generator config: NoGen"));
        assertFalse(Properties.Modes.containsKey("Bad"));
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveMode(null)).code());
        assertEquals(EditResult.Code.OK, run(ModeEdits.saveMode(new ModeInfo("Good", null, "Default", "shop", null, 100, 0, true))).code());
        assertFalse(ApiModeService.modes().stream().filter(m -> m.id().equals("Good")).findFirst().orElseThrow().builtIn());
    }

    private void withTablists() {
        when(plugin.getTablistManager()).thenReturn(new com.l299l.newbedwars.tablist.TablistManager(
                com.l299l.newbedwars.tablist.TablistConfig.parse(org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.StringReader("Layouts:\n  Default:\n    Playing:\n      Header: [a]\n  Fancy:\n    Playing:\n      Header: [b]\n")), null), null));
    }

    @Test
    void modeTablistRoundTrip() throws Exception {
        loadBundled();
        withTablists();
        assertEquals(EditResult.Code.OK, run(ModeEdits.saveMode(new ModeInfo("Fun", null, null, null, null, 0, 0, false, "fancy"))).code());
        assertEquals("Fancy", Properties.Modes.get("Fun").tablist());
        assertTrue(java.nio.file.Files.readString(dir.resolve("modes.yml")).contains("Tablist: \"Fancy\""));
        assertEquals("Fancy", ApiModeService.modes().stream().filter(m -> m.id().equals("Fun")).findFirst().orElseThrow().tablist());
        assertEquals(EditResult.Code.OK, run(ModeEdits.saveMode(info("Fun", null, null, 0, 0))).code());
        assertNull(Properties.Modes.get("Fun").tablist());
    }

    @Test
    void modeWithUnknownTablistIsInvalid() throws Exception {
        loadBundled();
        EditResult none = run(ModeEdits.saveMode(new ModeInfo("Fun", null, null, null, null, 0, 0, false, "Nope")));
        assertEquals(EditResult.Code.INVALID, none.code());
        assertEquals(List.of("unknown tablist: Nope"), none.problems());
        withTablists();
        assertEquals(List.of("unknown tablist: Nope"), run(ModeEdits.saveMode(new ModeInfo("Fun", null, null, null, null, 0, 0, false, "Nope"))).problems());
        assertFalse(Properties.Modes.containsKey("Fun"));
    }

    @Test
    void oldEightComponentConstructorMeansNoTablist() {
        assertNull(new ModeInfo("A", null, null, null, null, 0, 0, false).tablist());
    }

    @Test
    void deleteModeRules() throws Exception {
        loadBundled();
        assertEquals(EditResult.Code.NOT_FOUND, run(ModeEdits.deleteMode("Ghost")).code());
        assertEquals(EditResult.Code.NOT_FOUND, run(ModeEdits.deleteMode(null)).code());
        assertEquals(EditResult.Code.BUILT_IN, run(ModeEdits.deleteMode("squads")).code());
        assertTrue(Properties.Modes.containsKey("Squads"));
        run(ModeEdits.saveMode(info("Fun", null, null, 2, 0)));
        assertEquals(EditResult.Code.OK, run(ModeEdits.deleteMode("FUN")).code());
        assertFalse(Properties.Modes.containsKey("Fun"));
        assertFalse(java.nio.file.Files.readString(dir.resolve("modes.yml")).contains("Fun:"));
    }

    private static PhaseListInfo list(String id, String... actions) {
        return new PhaseListInfo(id, List.of(new com.l299l.newbedwars.api.mode.PhaseInfo("One", 60, List.of(actions)),
                new com.l299l.newbedwars.api.mode.PhaseInfo("End", 0, List.of("GAME_END:DRAW"))));
    }

    @Test
    void savePhaseListValidatesAndStores() throws Exception {
        loadBundled();
        assertEquals(EditResult.Code.OK, run(ModeEdits.savePhaseList(list("Quick", "DIAMOND_GENERATOR:2", "bed_destruction"))).code());
        List<PhaseListInfo> lists = ApiModeService.phaseLists();
        PhaseListInfo quick = lists.stream().filter(l -> l.id().equals("Quick")).findFirst().orElseThrow();
        assertEquals(2, quick.phases().size());
        assertEquals(60, quick.phases().get(0).duration());
        assertEquals(List.of("DIAMOND_GENERATOR:2", "bed_destruction"), quick.phases().get(0).actions());
        assertEquals(EditResult.Code.OK, run(ModeEdits.savePhaseList(list("quick", "BED_DESTRUCTION"))).code());
        assertEquals(1, Properties.GamePhases.keySet().stream().filter(k -> k.equalsIgnoreCase("quick")).count());

        EditResult bad = run(ModeEdits.savePhaseList(list("Bad", "NOPE:1", "DIAMOND_GENERATOR:x")));
        assertEquals(EditResult.Code.INVALID, bad.code());
        assertEquals(2, bad.problems().size());
        assertTrue(bad.problems().get(0).contains("NOPE:1"));
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.savePhaseList(new PhaseListInfo("Empty", List.of()))).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.savePhaseList(new PhaseListInfo("Neg",
                List.of(new com.l299l.newbedwars.api.mode.PhaseInfo("A", -1, List.of()))))).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.savePhaseList(list("a b"))).code());
        assertFalse(Properties.GamePhases.containsKey("Bad"));
    }

    @Test
    void deletePhaseListRules() throws Exception {
        loadBundled();
        assertEquals(EditResult.Code.NOT_FOUND, run(ModeEdits.deletePhaseList("Ghost")).code());
        assertEquals(EditResult.Code.BUILT_IN, run(ModeEdits.deletePhaseList("Solo")).code());
        assertEquals(EditResult.Code.BUILT_IN, run(ModeEdits.deletePhaseList("Default")).code());
        run(ModeEdits.savePhaseList(list("Quick")));
        run(ModeEdits.saveMode(info("Fun", null, "Quick", 0, 0)));
        when(arena.getGamePhasesId()).thenReturn("Quick");
        EditResult used = run(ModeEdits.deletePhaseList("Quick"));
        assertEquals(EditResult.Code.IN_USE, used.code());
        assertEquals(List.of("mode Fun", "arena castle"), used.usedBy());
        run(ModeEdits.deleteMode("Fun"));
        when(arena.getGamePhasesId()).thenReturn(null);
        assertEquals(EditResult.Code.OK, run(ModeEdits.deletePhaseList("Quick")).code());
        assertFalse(Properties.GamePhases.containsKey("Quick"));
        assertTrue(Properties.GamePhases.containsKey("Solo"));
    }

    private static String generatorYaml(String id) {
        return "ConfigurationId: " + id + "\n"
                + "TeamGenerator:\n  MaxLevel: 2\n  Iron:\n    SpawnRate: [1, 1]\n    MaxAmount: [1, 2]\n  SpawnDelay: [1, 1]\n"
                + "DiamondGenerator:\n  MaxLevel: 1\n  SpawnDelay: [1]\nEmeraldGenerator:\n  MaxLevel: 1\n  SpawnDelay: [1]\n";
    }

    @Test
    void generatorConfigSaveValidAndInvalid() throws Exception {
        java.nio.file.Path folder = dir.resolve("generators");
        java.nio.file.Files.createDirectories(folder);
        EditResult first = run(ModeEdits.saveGeneratorConfig("Mine", generatorYaml("Mine")));
        assertEquals(EditResult.Code.OK, first.code(), first.problems().toString());
        assertEquals(generatorYaml("Mine"), java.nio.file.Files.readString(folder.resolve("Mine.yml")));
        verify(plugin).reloadGeneratorConfigs();
        assertEquals(List.of(new com.l299l.newbedwars.api.mode.GeneratorConfigInfo("Mine", "Mine.yml", false)), ModeEdits.generatorFiles());
        assertEquals(generatorYaml("Mine"), ModeEdits.generatorText("Mine"));
        assertEquals(null, ModeEdits.generatorText("Nope"));

        java.nio.file.Files.writeString(folder.resolve("odd.yml"), generatorYaml("Odd"));
        String changed = generatorYaml("Odd").replace("[1, 2]", "[1, 3]");
        assertEquals(EditResult.Code.OK, run(ModeEdits.saveGeneratorConfig("Odd", changed)).code());
        assertEquals(changed, java.nio.file.Files.readString(folder.resolve("odd.yml")));
        assertFalse(java.nio.file.Files.exists(folder.resolve("Odd.yml")));
        assertEquals(List.of("Mine", "Odd"), ModeEdits.generatorFiles().stream().map(com.l299l.newbedwars.api.mode.GeneratorConfigInfo::id).toList());

        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveGeneratorConfig("Mine", generatorYaml("Other"))).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveGeneratorConfig("Mine", "a: [")).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveGeneratorConfig("Mine", "- a")).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveGeneratorConfig("Mine", "")).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveGeneratorConfig("Mine", "ConfigurationId: Mine\n")).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveGeneratorConfig("Mine",
                generatorYaml("Mine").replace("[1, 1]", "[1, 1, 1]"))).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveGeneratorConfig("Mine", generatorYaml("Mine") + "#".repeat(70000))).code());
        assertEquals(EditResult.Code.INVALID, run(ModeEdits.saveGeneratorConfig("../x", generatorYaml("../x"))).code());
        assertEquals(generatorYaml("Mine"), java.nio.file.Files.readString(folder.resolve("Mine.yml")));
        verify(plugin, org.mockito.Mockito.times(2)).reloadGeneratorConfigs();
    }

    @Test
    void generatorConfigDeleteRules() throws Exception {
        java.nio.file.Path folder = dir.resolve("generators");
        java.nio.file.Files.createDirectories(folder);
        java.nio.file.Files.writeString(folder.resolve("Mine.yml"), generatorYaml("Mine"));
        java.nio.file.Files.writeString(folder.resolve("Solo.yml"), generatorYaml("Solo"));
        assertEquals(EditResult.Code.NOT_FOUND, run(ModeEdits.deleteGeneratorConfig("Mine")).code());
        levelings.put("Mine", mock(GeneratorLeveling.class));
        levelings.put("Solo", mock(GeneratorLeveling.class));
        levelings.put("Ghost", mock(GeneratorLeveling.class));
        assertEquals(EditResult.Code.BUILT_IN, run(ModeEdits.deleteGeneratorConfig("Solo")).code());
        assertTrue(java.nio.file.Files.exists(folder.resolve("Solo.yml")));
        mode("Fun", "Mine", null, 0, 0);
        when(arena.getGeneratorsLevelingId()).thenReturn("Mine");
        EditResult used = run(ModeEdits.deleteGeneratorConfig("Mine"));
        assertEquals(EditResult.Code.IN_USE, used.code());
        assertEquals(List.of("mode Fun", "arena castle"), used.usedBy());
        assertTrue(java.nio.file.Files.exists(folder.resolve("Mine.yml")));
        Properties.Modes.clear();
        when(arena.getGeneratorsLevelingId()).thenReturn(null);
        assertEquals(EditResult.Code.NOT_FOUND, run(ModeEdits.deleteGeneratorConfig("Ghost")).code());
        assertEquals(EditResult.Code.OK, run(ModeEdits.deleteGeneratorConfig("Mine")).code());
        assertFalse(java.nio.file.Files.exists(folder.resolve("Mine.yml")));
        verify(plugin).reloadGeneratorConfigs();
    }

    @Test
    void publicEditMethodsCompleteOnTheMainThread() throws Exception {
        loadBundled();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            assertEquals(EditResult.Code.NOT_FOUND, new ApiModeService().deleteMode("Ghost").get().code());
            assertEquals(EditResult.Code.INVALID, new ApiModeService().saveMode(info("a b", null, null, 0, 0)).get().code());
            assertEquals(EditResult.Code.NOT_FOUND, new ApiModeService().deletePhaseList("Ghost").get().code());
            assertEquals(EditResult.Code.NOT_FOUND, new ApiModeService().deleteGeneratorConfig("Ghost").get().code());
            assertEquals(EditResult.Code.INVALID, new ApiModeService().saveGeneratorConfig("x", "a: [").get().code());
        }
    }
}
