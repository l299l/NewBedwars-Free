package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.mode.EditResult;
import com.l299l.newbedwars.api.mode.PhaseInfo;
import com.l299l.newbedwars.api.mode.PhaseListInfo;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.config.ModesFile;
import com.l299l.newbedwars.config.properties.GameMode;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUIManager;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ModeEditsTest {
    private Map<String, GameMode> savedModes;
    private HashMap<String, com.l299l.newbedwars.arena.phases.GamePhases> savedPhases;
    private NewBedwars plugin;

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path dir;

    @BeforeEach
    void setUp() {
        plugin = mock(NewBedwars.class);
        when(plugin.getGeneratorLeveling()).thenReturn(new HashMap<String, GeneratorLeveling>());
        when(plugin.getGuiManager()).thenReturn(mock(GUIManager.class));
        when(plugin.getDataFolder()).thenReturn(dir.toFile());
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        NewBedwars.plugin = plugin;
        savedModes = Properties.Modes;
        savedPhases = Properties.GamePhases;
        Properties.Modes = new LinkedHashMap<>();
        Properties.GamePhases = new HashMap<>();
        ArenaRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        Properties.Modes = savedModes;
        Properties.GamePhases = savedPhases;
        ModesFile.use(Map.of(), Map.of());
        ArenaRegistry.clear();
        NewBedwars.plugin = null;
    }

    private static PhaseListInfo list(PhaseInfo... phases) {
        return new PhaseListInfo("Quick", List.of(phases));
    }

    @Test
    void resultHelpers() {
        assertEquals(EditResult.Code.OK, ModeEdits.ok().code());
        assertEquals(EditResult.Code.NOT_FOUND, ModeEdits.code(EditResult.Code.NOT_FOUND).code());
        assertEquals(List.of("x"), ModeEdits.invalid(List.of("x")).problems());
        assertEquals(List.of("boom"), ModeEdits.failed(new IOException("boom")).problems());
        assertEquals(EditResult.Code.FAILED, ModeEdits.failed(new IllegalStateException()).code());
        assertEquals(1, ModeEdits.failed(new IllegalStateException()).problems().size());
    }

    @Test
    void planRunNowReturnsEarlyResultWithoutRunningIo() {
        EditResult early = ModeEdits.code(EditResult.Code.BUILT_IN);
        assertSame(early, ModeEdits.Plan.done(early).runNow());
    }

    @Test
    void planRunNowReloadsOnlyAfterSuccessfulIo() {
        AtomicInteger reloads = new AtomicInteger();
        assertEquals(EditResult.Code.OK, new ModeEdits.Plan(null, ModeEdits::ok, reloads::incrementAndGet).runNow().code());
        assertEquals(1, reloads.get());
        assertEquals(EditResult.Code.INVALID,
                new ModeEdits.Plan(null, () -> ModeEdits.invalid(List.of("no")), reloads::incrementAndGet).runNow().code());
        assertEquals(1, reloads.get());
    }

    @Test
    void planRunNowMapsIoFailuresToFailed() {
        AtomicInteger reloads = new AtomicInteger();
        EditResult io = new ModeEdits.Plan(null, () -> {
            throw new IOException("disk");
        }, reloads::incrementAndGet).runNow();
        assertEquals(EditResult.Code.FAILED, io.code());
        assertEquals(List.of("disk"), io.problems());
        EditResult rt = new ModeEdits.Plan(null, () -> {
            throw new IllegalArgumentException("bad");
        }, reloads::incrementAndGet).runNow();
        assertEquals(EditResult.Code.FAILED, rt.code());
        assertEquals(0, reloads.get());
    }

    @Test
    void executeReturnsEarlyResultImmediately() throws Exception {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            CompletableFuture<EditResult> future = ModeEdits.execute(() -> ModeEdits.Plan.done(ModeEdits.code(EditResult.Code.NOT_FOUND)));
            assertEquals(EditResult.Code.NOT_FOUND, future.get(5, TimeUnit.SECONDS).code());
        }
    }

    @Test
    void phaseListValidationReportsEveryProblem() {
        List<String> problems = ModeEdits.validatePhaseList(new PhaseListInfo("a b", List.of(
                new PhaseInfo(" ", -5, List.of("", "BED_DESTRUCTION\nX", "EMERALD_GENERATOR:x")))));
        assertEquals(6, problems.size(), problems.toString());
        assertTrue(problems.get(0).startsWith("invalid phase list id"));
        assertTrue(problems.contains("phase 1 has no name"));
        assertTrue(problems.contains("phase 1 has a negative duration"));
        assertTrue(problems.stream().anyMatch(p -> p.contains("invalid action")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("needs a level number")));
    }

    @Test
    void validPhaseListHasNoProblemsAndEmptyListIsRejected() {
        assertTrue(ModeEdits.validatePhaseList(list(new PhaseInfo("A", 0, List.of("DIAMOND_GENERATOR:2", "bed_destruction")))).isEmpty());
        assertEquals(List.of("a phase list needs at least one phase"), ModeEdits.validatePhaseList(list()));
    }

    @Test
    void builtInPhaseListsContainModeNamesAndBundledIds() {
        ModesFile.use(Map.of(), Map.of(ModesFile.PHASES, List.of(Map.of("ID", "Fancy"), Map.of("Other", 1))));
        Set<String> builtIn = ModeEdits.builtInPhaseLists();
        assertTrue(builtIn.containsAll(List.of("Solo", "Doubles", "Squads", "Rush", "Duels", "Fancy")));
        assertFalse(builtIn.contains("Other"));
    }

    @Test
    void deleteRefusalsNeedNoIo() {
        Properties.Modes.put("Solo", new GameMode("Solo", null, null, null, null, 0, 0));
        assertEquals(EditResult.Code.BUILT_IN, ModeEdits.deleteMode("solo").runNow().code());
        assertEquals(EditResult.Code.NOT_FOUND, ModeEdits.deleteMode("Ghost").runNow().code());
        assertEquals(EditResult.Code.NOT_FOUND, ModeEdits.deletePhaseList(null).runNow().code());
        assertEquals(EditResult.Code.NOT_FOUND, ModeEdits.deleteGeneratorConfig(null).runNow().code());
    }

    @Test
    void phaseListInUseByModeCannotBeDeleted() {
        Properties.GamePhases.put("Quick", new com.l299l.newbedwars.arena.phases.GamePhases("Quick", List.of()));
        Properties.Modes.put("Fun", new GameMode("Fun", null, "Quick", null, null, 0, 0));
        EditResult result = ModeEdits.deletePhaseList("quick").runNow();
        assertEquals(EditResult.Code.IN_USE, result.code());
        assertEquals(List.of("mode Fun"), result.usedBy());
    }

    @Test
    void saveGeneratorConfigRejectsBadIdWithoutTouchingDisk() {
        EditResult result = ModeEdits.saveGeneratorConfig("a/b", "x: 1").runNow();
        assertEquals(EditResult.Code.INVALID, result.code());
        assertFalse(java.nio.file.Files.exists(dir.resolve("generators")));
    }

    @Test
    void validateModeAcceptsBlankReferencesAndRangeEdges() {
        assertTrue(ModeEdits.validateMode(new com.l299l.newbedwars.api.mode.ModeInfo("Ok", " ", "", null, null, 0, 100, false)).isEmpty());
        assertEquals(2, ModeEdits.validateMode(new com.l299l.newbedwars.api.mode.ModeInfo("Ok", null, null, null, null, -1, 101, false)).size());
    }
}
