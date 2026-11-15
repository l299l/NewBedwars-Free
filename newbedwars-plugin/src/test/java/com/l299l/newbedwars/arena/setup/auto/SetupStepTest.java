package com.l299l.newbedwars.arena.setup.auto;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SetupStepTest {

    private static Team team(boolean complete) {
        Team team = mock(Team.class);
        when(team.isBedSet()).thenReturn(complete);
        when(team.isSpawnSet()).thenReturn(complete);
        when(team.isShopSet()).thenReturn(complete);
        when(team.isUpgradesSet()).thenReturn(complete);
        when(team.isGeneratorSet()).thenReturn(complete);
        when(team.isBasePosSet()).thenReturn(complete);
        when(team.isBuildProtAreaPosSet()).thenReturn(complete);
        return team;
    }

    private static IArena arena(Map<String, Team> teams, boolean generator, boolean waiting, Integer quickVoid) {
        IArena arena = mock(IArena.class);
        when(arena.getTeams()).thenReturn(teams);
        List<Generator> generators = new ArrayList<>();
        if (generator) generators.add(mock(Generator.class));
        when(arena.getGenerators()).thenReturn(generators);
        if (waiting) {
            when(arena.getWaitingPos1()).thenReturn(mock(Location.class));
            when(arena.getWaitingPos2()).thenReturn(mock(Location.class));
            when(arena.getWaitingSpawn()).thenReturn(mock(Location.class));
        }
        when(arena.getQuickVoidY()).thenReturn(quickVoid);
        return arena;
    }

    @Test
    void nothingMissingMeansNoSteps() {
        Map<String, Team> teams = new HashMap<>();
        teams.put("Red", team(true));
        teams.put("Blue", team(true));
        assertTrue(SetupStep.missing(arena(teams, true, true, 0)).isEmpty());
    }

    @Test
    void emptyArenaAsksForTeamsThenGlobalSettingsInOrder() {
        List<SetupStep> steps = SetupStep.missing(arena(new HashMap<>(), false, false, null));
        assertEquals(List.of(SetupStep.Kind.TEAMS, SetupStep.Kind.ARENA_GENERATOR, SetupStep.Kind.WAITING_POS1,
                SetupStep.Kind.WAITING_POS2, SetupStep.Kind.WAITING_SPAWN, SetupStep.Kind.QUICK_VOID),
                steps.stream().map(SetupStep::kind).toList());
    }

    @Test
    void teamStepsComeInOrderPerTeamSortedByName() {
        Map<String, Team> teams = new HashMap<>();
        teams.put("Red", team(false));
        teams.put("Blue", team(true));
        List<SetupStep> steps = SetupStep.missing(arena(teams, true, true, 0));
        assertEquals(7, steps.size());
        assertEquals(new SetupStep(SetupStep.Kind.TEAM_BED, "Red"), steps.get(0));
        assertEquals(new SetupStep(SetupStep.Kind.TEAM_SPAWN, "Red"), steps.get(1));
        assertEquals(new SetupStep(SetupStep.Kind.TEAM_PROTECTION, "Red"), steps.get(6));
    }

    @Test
    void partiallySetTeamOnlyAsksForTheRest() {
        Team red = team(true);
        when(red.isShopSet()).thenReturn(false);
        Map<String, Team> teams = new HashMap<>();
        teams.put("Red", red);
        teams.put("Blue", team(true));
        List<SetupStep> steps = SetupStep.missing(arena(teams, true, true, 0));
        assertEquals(List.of(new SetupStep(SetupStep.Kind.TEAM_SHOP, "Red")), steps);
    }

    @Test
    void everyStepHasAButtonAndKeyIsUnique() {
        for (SetupStep.Kind kind : SetupStep.Kind.values()) {
            SetupStep step = new SetupStep(kind, "Red");
            assertFalse(AutoSetupSession.buttons(step).isEmpty(), kind.name());
        }
        assertNotEquals(new SetupStep(SetupStep.Kind.TEAM_BED, "Red").key(), new SetupStep(SetupStep.Kind.TEAM_BED, "Blue").key());
    }

    @Test
    void buttonsRunTheExistingSetupCommands() {
        assertEquals("/bw setTeamShop Red VILLAGER", AutoSetupSession.buttons(new SetupStep(SetupStep.Kind.TEAM_SHOP, "Red")).get(0).command());
        assertEquals("/bw setGenerator BASIC Red", AutoSetupSession.buttons(new SetupStep(SetupStep.Kind.TEAM_GENERATOR, "Red")).get(0).command());
        assertEquals("/bw setQuickVoidY", AutoSetupSession.buttons(new SetupStep(SetupStep.Kind.QUICK_VOID, null)).get(0).command());
    }
}
