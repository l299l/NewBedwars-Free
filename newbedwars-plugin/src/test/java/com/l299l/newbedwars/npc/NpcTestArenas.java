package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class NpcTestArenas {
    private NpcTestArenas() {}

    static IArena arena(String name, GameStatus status, int teams, int maxInTeam, int players) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn(name);
        when(arena.isEnabled()).thenReturn(true);
        when(arena.status()).thenReturn(status);
        Map<String, Team> teamMap = new HashMap<>();
        for (int i = 0; i < teams; i++) teamMap.put("t" + i, mock(Team.class));
        when(arena.getTeams()).thenReturn(teamMap);
        when(arena.getMaxInTeam()).thenReturn(maxInTeam);
        when(arena.getMaxPlayers()).thenCallRealMethod();
        when(arena.getActivePlayerCount()).thenReturn(players);
        when(arena.isPlayerInArena(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        return arena;
    }
}
