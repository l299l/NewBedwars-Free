package com.l299l.newbedwars.config.data.json.arenas;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArenaValidatorTest {
    private Team team(String name, boolean spawn) {
        Team t = mock(Team.class);
        when(t.getName()).thenReturn(name);
        when(t.getColor()).thenReturn(ChatColor.RED);
        when(t.getTeamSpawn()).thenReturn(spawn ? mock(Location.class) : null);
        return t;
    }

    private IArena arena(HashMap<String, Team> teams, boolean waitingSpawn) {
        IArena a = mock(IArena.class);
        when(a.getArenaWorld()).thenReturn(mock(World.class));
        when(a.getWaitingSpawn()).thenReturn(waitingSpawn ? mock(Location.class) : null);
        when(a.getTeams()).thenReturn(teams);
        return a;
    }

    @Test
    void emptyArenaReportsMissingPieces() {
        List<String> problems = ArenaValidator.validate(arena(new HashMap<>(), false));
        assertTrue(problems.stream().anyMatch(p -> p.contains("waiting spawn")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("fewer than 2 teams")));
    }

    @Test
    void teamWithoutSpawnAndBedIsReported() {
        HashMap<String, Team> teams = new HashMap<>();
        teams.put("red", team("red", false));
        teams.put("blue", team("blue", true));
        List<String> problems = ArenaValidator.validate(arena(teams, true));
        assertTrue(problems.contains("team red has no spawn"));
        assertTrue(problems.contains("team red has no bed"));
        assertFalse(problems.contains("team blue has no spawn"));
    }
}
