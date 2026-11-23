package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
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

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        levelings = new HashMap<>();
        when(plugin.getGeneratorLeveling()).thenReturn(levelings);
        when(plugin.getGuiManager()).thenReturn(mock(GUIManager.class));
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
}
