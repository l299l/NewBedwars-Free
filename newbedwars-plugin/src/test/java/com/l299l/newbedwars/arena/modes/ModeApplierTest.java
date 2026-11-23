package com.l299l.newbedwars.arena.modes;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.config.properties.GameMode;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUIManager;
import com.l299l.newbedwars.gui.GUISave;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModeApplierTest {
    private GUIManager guiManager;
    private HashMap<String, GeneratorLeveling> levelings;
    private Map<String, GameMode> savedModes;
    private HashMap<String, GamePhases> savedPhases;
    private GeneratorLeveling leveling;
    private GamePhases phases;
    private IArena arena;

    @BeforeEach
    void setUp() {
        guiManager = mock(GUIManager.class);
        levelings = new HashMap<>();
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getGuiManager()).thenReturn(guiManager);
        when(plugin.getGeneratorLeveling()).thenReturn(levelings);
        NewBedwars.plugin = plugin;
        savedModes = Properties.Modes;
        savedPhases = Properties.GamePhases;
        Properties.Modes = new LinkedHashMap<>();
        Properties.GamePhases = new HashMap<>();
        leveling = mock(GeneratorLeveling.class);
        phases = new GamePhases("Squads", List.of());
        levelings.put("Squads", leveling);
        Properties.GamePhases.put("Squads", phases);
        when(guiManager.getGui("shopX")).thenReturn(mock(GUISave.class));
        when(guiManager.getGui("upX")).thenReturn(mock(GUISave.class));
        arena = mock(IArena.class);
        Map<String, Team> teams = new HashMap<>();
        for (int i = 0; i < 4; i++) teams.put("t" + i, mock(Team.class));
        when(arena.getTeams()).thenReturn(teams);
    }

    @AfterEach
    void tearDown() {
        Properties.Modes = savedModes;
        Properties.GamePhases = savedPhases;
        NewBedwars.plugin = null;
    }

    private GameMode put(GameMode mode) {
        Properties.Modes.put(mode.id(), mode);
        return mode;
    }

    @Test
    void modeTablistIsCopiedToTheArena() {
        GameMode mode = put(new GameMode("T", null, null, null, null, 0, 0, "Fancy"));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            assertTrue(ModeApplier.apply(arena, mode).applied());
        }
        verify(arena).setTablistId("Fancy");
    }

    @Test
    void findExactThenCaseInsensitive() {
        GameMode squads = put(new GameMode("Squads", null, null, null, null, 0, 0));
        GameMode lower = put(new GameMode("squads", null, null, null, null, 0, 0));
        assertSame(squads, ModeApplier.find("Squads"));
        assertSame(lower, ModeApplier.find("squads"));
        assertSame(squads, ModeApplier.find("SQUADS"));
    }

    @Test
    void findUnknownOrNullIsNull() {
        put(new GameMode("Squads", null, null, null, null, 0, 0));
        assertNull(ModeApplier.find("Nope"));
        assertNull(ModeApplier.find(null));
    }

    @Test
    void appliesEveryPartAndSaves() {
        GameMode mode = put(new GameMode("Squads", "Squads", "Squads", "shopX", "upX", 4, 4));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            ModeApplier.Result result = ModeApplier.apply(arena, mode);
            assertTrue(result.applied());
            assertTrue(result.missing().isEmpty());
            assertFalse(result.teamsDiffer());
            assertEquals(4, result.arenaTeams());
            assertEquals(1, json.constructed().size());
            verify(json.constructed().get(0)).save();
        }
        verify(arena).setGeneratorsLeveling(leveling);
        verify(arena).setGamePhases(phases);
        verify(arena).setShopGuiId("shopX");
        verify(arena).setUpgradeGuiId("upX");
        verify(arena).setMaxInTeam(4);
    }

    @Test
    void partialModeOnlySetsPresentParts() {
        GameMode mode = put(new GameMode("P", null, "Squads", null, null, 0, 0));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            assertTrue(ModeApplier.apply(arena, mode).applied());
            verify(json.constructed().get(0)).save();
        }
        verify(arena).setGamePhases(phases);
        verify(arena, never()).setGeneratorsLeveling(any());
        verify(arena, never()).setShopGuiId(anyString());
        verify(arena, never()).setUpgradeGuiId(anyString());
        verify(arena, never()).setMaxInTeam(anyInt());
    }

    @Test
    void missingPartsChangeNothing() {
        GameMode mode = put(new GameMode("B", "NoGen", "Squads", "shopX", "noUp", 3, 0));
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            ModeApplier.Result result = ModeApplier.apply(arena, mode);
            assertFalse(result.applied());
            assertEquals(List.of("NoGen", "noUp"), result.missing());
            assertFalse(result.teamsDiffer());
            assertEquals(4, result.arenaTeams());
            assertTrue(json.constructed().isEmpty());
        }
        verify(arena, never()).setGeneratorsLeveling(any());
        verify(arena, never()).setGamePhases(any());
        verify(arena, never()).setShopGuiId(anyString());
        verify(arena, never()).setMaxInTeam(anyInt());
    }

    @Test
    void missingPhasesAndShopAreListed() {
        GameMode mode = put(new GameMode("B", null, "NoPhases", "noShop", null, 0, 0));
        ModeApplier.Result result = ModeApplier.apply(arena, mode);
        assertFalse(result.applied());
        assertEquals(List.of("NoPhases", "noShop"), result.missing());
    }

    @Test
    void teamsDifferOnlyWhenModeSpecifiesAnotherCount() {
        try (MockedConstruction<ArenaDataJson> json = mockConstruction(ArenaDataJson.class)) {
            assertTrue(ModeApplier.apply(arena, put(new GameMode("D", null, null, null, null, 0, 2))).teamsDiffer());
            assertFalse(ModeApplier.apply(arena, put(new GameMode("E", null, null, null, null, 0, 4))).teamsDiffer());
            assertFalse(ModeApplier.apply(arena, put(new GameMode("F", null, null, null, null, 0, 0))).teamsDiffer());
        }
    }
}
