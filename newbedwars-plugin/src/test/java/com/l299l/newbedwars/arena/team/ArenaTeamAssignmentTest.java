package com.l299l.newbedwars.arena.team;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ArenaTeamAssignmentTest {

    private static List<String> group(String... names) {
        return new ArrayList<>(List.of(names));
    }

    private static int total(List<List<String>> teams) {
        return teams.stream().mapToInt(List::size).sum();
    }

    @Test
    void soloPlayersAreSpreadEvenly() {
        List<List<String>> groups = List.of(group("a"), group("b"), group("c"), group("d"));
        List<List<String>> res = TeamAssigner.assign(groups, 4, 2, false, new Random(1));
        assertEquals(4, total(res));
        res.forEach(t -> assertEquals(1, t.size()));
    }

    @Test
    void partyStaysTogether() {
        List<List<String>> groups = List.of(group("a", "b"), group("c"), group("d"));
        List<List<String>> res = TeamAssigner.assign(groups, 2, 2, false, new Random(1));
        assertTrue(res.stream().anyMatch(t -> t.containsAll(List.of("a", "b"))));
        assertEquals(4, total(res));
    }

    @Test
    void oversizedPartyIsSplit() {
        List<List<String>> groups = List.of(group("a", "b", "c"));
        List<List<String>> res = TeamAssigner.assign(groups, 2, 2, false, new Random(1));
        assertEquals(3, total(res));
        res.forEach(t -> assertTrue(t.size() <= 2));
    }

    @Test
    void neverExceedsMaxInTeam() {
        List<List<String>> groups = List.of(group("a", "b"), group("c", "d"), group("e", "f"), group("g"));
        List<List<String>> res = TeamAssigner.assign(groups, 4, 2, true, new Random(7));
        assertEquals(7, total(res));
        res.forEach(t -> assertTrue(t.size() <= 2));
    }

    @Test
    void noTeamsYieldsEmptyResult() {
        assertTrue(TeamAssigner.assign(List.of(group("a")), 0, 2, false, new Random()).isEmpty());
    }

    @Test
    void playersBeyondCapacityAreDropped() {
        List<List<String>> groups = List.of(group("a"), group("b"), group("c"));
        List<List<String>> res = TeamAssigner.assign(groups, 1, 2, false, new Random(1));
        assertEquals(2, total(res));
    }
}
