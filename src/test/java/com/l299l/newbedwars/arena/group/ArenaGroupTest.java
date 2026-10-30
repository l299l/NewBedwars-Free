package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArenaGroupTest {

    static IArena arena(String name, int teams, int maxInTeam) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn(name);
        Map<String, Team> teamMap = new HashMap<>();
        for (int i = 0; i < teams; i++) teamMap.put("t" + i, mock(Team.class));
        when(arena.getTeams()).thenReturn(teamMap);
        when(arena.getMaxInTeam()).thenReturn(maxInTeam);
        when(arena.getMaxPlayers()).thenCallRealMethod();
        return arena;
    }

    @Test
    void membershipIgnoresCase() {
        ArenaGroup g = new ArenaGroup("solo", null);
        g.add("Castle");
        g.add("castle");
        assertEquals(List.of("Castle"), g.getArenas());
        assertTrue(g.contains("CASTLE"));
        assertTrue(g.remove("cAsTlE"));
        assertFalse(g.contains("Castle"));
        assertEquals(ArenaGroup.DEFAULT_ITEM, g.getDisplayItem());
    }

    @Test
    void arenasMustHaveTheSameTeamSize() {
        IArena castle = arena("castle", 4, 1);
        IArena ruins = arena("ruins", 4, 1);
        IArena duo = arena("duo", 4, 2);
        Map<String, IArena> all = Map.of("castle", castle, "ruins", ruins, "duo", duo);
        ArenaGroup g = new ArenaGroup("solo", "MAP");
        g.add("castle");
        assertNull(g.incompatibility(ruins, all::get));
        String reason = g.incompatibility(duo, all::get);
        assertNotNull(reason);
        assertTrue(reason.contains("4x1") && reason.contains("4x2"), reason);
    }

    @Test
    void problemsListMissingAndMismatchedArenas() {
        IArena castle = arena("castle", 4, 1);
        IArena duo = arena("duo", 4, 2);
        Map<String, IArena> all = Map.of("castle", castle, "duo", duo);
        ArenaGroup g = new ArenaGroup("solo", null);
        g.add("castle");
        assertTrue(g.problems(all::get).stream().anyMatch(p -> p.contains("at least 2")));
        g.add("duo");
        g.add("ghost");
        List<String> problems = g.problems(all::get);
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("ghost")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("duo")));
        assertEquals(List.of(castle, duo), g.resolve(all::get));
    }

    @Test
    void storeRoundTrip(@TempDir Path dir) throws Exception {
        ArenaGroup g = new ArenaGroup("solo", "MAP");
        g.add("castle");
        g.add("ruins");
        Path file = dir.resolve("groups.json");
        assertTrue(ArenaGroupStore.load(file).isEmpty());
        ArenaGroupStore.save(file, List.of(g), dir.resolve("backups"), 3);
        List<ArenaGroup> back = ArenaGroupStore.load(file);
        assertEquals(1, back.size());
        assertEquals("solo", back.get(0).getName());
        assertEquals(List.of("castle", "ruins"), back.get(0).getArenas());
        assertEquals("MAP", back.get(0).getDisplayItem());
    }

    @Test
    void storeSkipsGroupsWithoutName() {
        List<ArenaGroup> back = ArenaGroupStore.fromJson("{\"version\":1,\"groups\":[{\"arenas\":[\"a\"]},{\"name\":\"ok\"}]}");
        assertEquals(1, back.size());
        assertEquals("ok", back.get(0).getName());
        assertTrue(back.get(0).getArenas().isEmpty());
    }
}
