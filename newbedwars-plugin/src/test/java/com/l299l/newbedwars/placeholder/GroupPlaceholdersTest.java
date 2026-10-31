package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.team.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupPlaceholdersTest {

    private static IArena arena(String name, GameStatus status, int players) {
        IArena a = mock(IArena.class);
        when(a.getArenaName()).thenReturn(name);
        when(a.isEnabled()).thenReturn(true);
        when(a.status()).thenReturn(status);
        when(a.getActivePlayerCount()).thenReturn(players);
        Map<String, Team> teams = new HashMap<>();
        for (int i = 0; i < 4; i++) teams.put("t" + i, mock(Team.class));
        when(a.getTeams()).thenReturn(teams);
        when(a.getMaxInTeam()).thenReturn(1);
        when(a.getMaxPlayers()).thenCallRealMethod();
        ArenaRegistry.register(name, a);
        return a;
    }

    private IArena castle;

    @BeforeEach
    void setUp() {
        ArenaRegistry.clear();
        castle = arena("castle", GameStatus.playing, 3);
        arena("ruins", GameStatus.waiting, 2);
        arena("desert", GameStatus.starting, 4);   // full
        IArena off = arena("ice", GameStatus.waiting, 0);
        when(off.isEnabled()).thenReturn(false);
        ArenaGroupManager groups = new ArenaGroupManager();
        ArenaGroup g = new ArenaGroup("solo_4", null);  // underscore in the name on purpose
        for (String n : new String[]{"castle", "ruins", "desert", "ice", "ghost"}) g.add(n);
        groups.register(g);
    }

    @AfterEach
    void tearDown() {
        new ArenaGroupManager();
        ArenaRegistry.clear();
    }

    @Test
    void groupFields() {
        assertEquals("9", GroupPlaceholders.resolve("solo_4_players"));
        assertEquals("3", GroupPlaceholders.resolve("solo_4_playing"));
        assertEquals("6", GroupPlaceholders.resolve("solo_4_waiting"));
        assertEquals("12", GroupPlaceholders.resolve("solo_4_max_players"), "enabled arenas only");
        assertEquals("4", GroupPlaceholders.resolve("solo_4_arenas"), "existing arenas (ghost is missing)");
        assertEquals("1", GroupPlaceholders.resolve("solo_4_arenas_free"));
        assertEquals("ruins", GroupPlaceholders.resolve("solo_4_best_arena"));
    }

    @Test
    void unknownGroupIsEmptyAndUnknownFieldIsNull() {
        assertEquals("", GroupPlaceholders.resolve("duo_players"));
        assertNull(GroupPlaceholders.resolve("solo_4_colour"));
        assertNull(GroupPlaceholders.resolve("_players"));
    }

    @Test
    void groupOfPlayersArena() {
        assertEquals("solo_4", GroupPlaceholders.groupOf(castle));
        assertEquals("", GroupPlaceholders.groupOf(null));
        assertEquals("", GroupPlaceholders.groupOf(arena("alone", GameStatus.waiting, 0)));
    }
}
