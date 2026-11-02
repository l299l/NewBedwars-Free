package com.l299l.newbedwars.common.routing;

import com.l299l.newbedwars.common.routing.ServerSelector.Candidate;
import com.l299l.newbedwars.common.routing.ServerSelector.Choice;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ServerSelectorTest {

    private static ArenaInfo arena(String name, String group, ArenaInfo.State state, int players, int max, int perTeam, boolean parties) {
        return new ArenaInfo(name, group, state, players, max, perTeam, max / perTeam, parties);
    }

    private static Candidate server(String id, ArenaInfo... arenas) {
        return new Candidate(id, new ServerState(id, "GAME", false, 0, List.of(arenas)), true);
    }

    private static List<String> ids(List<Choice> choices) {
        return choices.stream().map(Choice::toString).collect(Collectors.toList());
    }

    @Test
    void fullestJoinableArenaFirst() {
        List<Choice> c = ServerSelector.choose(List.of(
                server("g1", arena("a", "solo", ArenaInfo.State.WAITING, 1, 8, 1, true),
                        arena("b", "solo", ArenaInfo.State.PLAYING, 4, 8, 1, true)),
                server("g2", arena("c", "solo", ArenaInfo.State.STARTING, 5, 8, 1, true),
                        arena("d", "solo", ArenaInfo.State.WAITING, 8, 8, 1, true))),
                TargetKind.ANY, null, 1, Set.of());
        assertEquals(List.of("g2/c", "g1/a"), ids(c));
    }

    @Test
    void tiesByName() {
        List<Choice> c = ServerSelector.choose(List.of(
                server("g1", arena("b", null, ArenaInfo.State.WAITING, 2, 8, 1, true)),
                server("g2", arena("A", null, ArenaInfo.State.WAITING, 2, 8, 1, true))),
                TargetKind.ANY, null, 1, Set.of());
        assertEquals(List.of("g2/A", "g1/b"), ids(c));
    }

    @Test
    void partySizeNeedsRoomPartiesAndTeamSize() {
        List<Candidate> s = List.of(server("g1",
                arena("solo", null, ArenaInfo.State.WAITING, 0, 8, 1, true),
                arena("noParty", null, ArenaInfo.State.WAITING, 0, 8, 2, false),
                arena("almostFull", null, ArenaInfo.State.WAITING, 7, 8, 2, true),
                arena("duos", null, ArenaInfo.State.WAITING, 0, 8, 2, true)));
        assertEquals(List.of("g1/duos"), ids(ServerSelector.choose(s, TargetKind.ANY, null, 2, Set.of())));
    }

    @Test
    void groupTargetUsesGroupThenPatternThenSize() {
        List<Candidate> s = List.of(server("g1",
                arena("x1", "solo", ArenaInfo.State.WAITING, 0, 8, 1, true),
                arena("solo_2", null, ArenaInfo.State.WAITING, 1, 8, 1, true),
                arena("quad", null, ArenaInfo.State.WAITING, 2, 16, 4, true)));
        assertEquals(List.of("g1/x1"), ids(ServerSelector.choose(s, TargetKind.GROUP, "SOLO", 1, Set.of())));
        assertEquals(List.of("g1/solo_2"), ids(ServerSelector.choose(s, TargetKind.GROUP, "solo_*", 1, Set.of())));
        assertEquals(List.of("g1/quad"), ids(ServerSelector.choose(s, TargetKind.GROUP, "4x4", 1, Set.of())));
        assertEquals(List.of(), ids(ServerSelector.choose(s, TargetKind.GROUP, "nothing", 1, Set.of())));
    }

    @Test
    void arenaTargetIsExactName() {
        List<Candidate> s = List.of(server("g1",
                arena("Lighthouse", null, ArenaInfo.State.WAITING, 0, 8, 1, true),
                arena("Lighthouse2", null, ArenaInfo.State.WAITING, 3, 8, 1, true)));
        assertEquals(List.of("g1/Lighthouse"), ids(ServerSelector.choose(s, TargetKind.ARENA, "lighthouse", 1, Set.of())));
    }

    @Test
    void staleServersAreBlindFallbackAfterMatches() {
        Candidate stale = new Candidate("g0", null, false);
        Candidate old = new Candidate("g3", new ServerState("g3", "GAME", false, 0, List.of()), false);
        List<Choice> c = ServerSelector.choose(List.of(stale, old,
                server("g1", arena("a", null, ArenaInfo.State.WAITING, 0, 8, 1, true))), TargetKind.ANY, null, 1, Set.of());
        assertEquals(List.of("g1/a", "g0", "g3"), ids(c));
        assertTrue(c.get(1).isBlind());
    }

    @Test
    void restartingAndExcludedServersAreSkipped() {
        Candidate restarting = new Candidate("g1", new ServerState("g1", "GAME", true, 0,
                List.of(arena("a", null, ArenaInfo.State.WAITING, 0, 8, 1, true))), true);
        Candidate g2 = server("g2", arena("b", null, ArenaInfo.State.WAITING, 0, 8, 1, true));
        assertEquals(List.of("g2/b"), ids(ServerSelector.choose(List.of(restarting, g2), TargetKind.ANY, null, 1, Set.of())));
        assertEquals(List.of(), ids(ServerSelector.choose(List.of(restarting, g2), TargetKind.ANY, null, 1, Set.of("g2"))));
    }

    @Test
    void noMatchGivesEmptyList() {
        assertTrue(ServerSelector.choose(List.of(), TargetKind.ANY, null, 1, Set.of()).isEmpty());
        assertTrue(ServerSelector.choose(List.of(server("g1",
                arena("a", null, ArenaInfo.State.ENDING, 0, 8, 1, true),
                arena("b", null, ArenaInfo.State.DISABLED, 0, 8, 1, true))), TargetKind.ANY, null, 1, Set.of()).isEmpty());
    }

    @Test
    void rejoinIsFoundBySlotAndNeverByTarget() {
        java.util.UUID me = java.util.UUID.randomUUID();
        ArenaInfo running = arena("r", "solo", ArenaInfo.State.PLAYING, 3, 8, 1, true);
        running.rejoin.add(new com.l299l.newbedwars.common.protocol.payload.RejoinSlot(me, 10_000));
        Candidate g2 = new Candidate("g2", new ServerState("g2", "GAME", false, 0, List.of(running)), false);
        Choice c = ServerSelector.findRejoin(List.of(server("g1", arena("a", null, ArenaInfo.State.WAITING, 0, 8, 1, true)), g2), me, 5_000);
        assertEquals("g2/r", c.toString());
        assertNull(ServerSelector.findRejoin(List.of(g2), me, 10_001), "expired");
        assertNull(ServerSelector.findRejoin(List.of(g2), java.util.UUID.randomUUID(), 0));
        assertTrue(ServerSelector.choose(List.of(g2), TargetKind.REJOIN, null, 1, Set.of()).stream().noneMatch(x -> !x.isBlind()));
    }

    @Test
    void findRunningPrefersFreshServers() {
        ArenaInfo run = arena("x", null, ArenaInfo.State.PLAYING, 4, 8, 1, true);
        ArenaInfo waiting = arena("y", null, ArenaInfo.State.WAITING, 0, 8, 1, true);
        Candidate stale = new Candidate("g1", new ServerState("g1", "GAME", false, 0, List.of(run)), false);
        Candidate fresh = server("g2", run, waiting);
        assertEquals("g2/x", ServerSelector.findRunning(List.of(stale, fresh), "X").toString());
        assertEquals("g1/x", ServerSelector.findRunning(List.of(stale), "x").toString());
        assertNull(ServerSelector.findRunning(List.of(fresh), "y"), "not running");
        assertNull(ServerSelector.findRunning(List.of(fresh), null));
        assertTrue(ServerSelector.choose(List.of(fresh), TargetKind.SPECTATE, "x", 1, Set.of()).isEmpty());
    }

    @Test
    void globSupportsStarsLiterallyAndCaseInsensitive() {
        assertTrue(ServerSelector.glob("solo*", "Solo_12"));
        assertTrue(ServerSelector.glob("*_2", "solo_2"));
        assertTrue(ServerSelector.glob("s*o*2", "solo_2"));
        assertTrue(ServerSelector.glob("*", ""));
        assertTrue(ServerSelector.glob("**a**", "a"));
        assertTrue(ServerSelector.glob("a.c", "A.C"));
        assertFalse(ServerSelector.glob("a.c", "abc"), "dots are literal");
        assertFalse(ServerSelector.glob("solo", "solo_2"));
        assertFalse(ServerSelector.glob("solo*x", "solo_2"));
        assertFalse(ServerSelector.glob("a(b", "ab"));
        assertTrue(ServerSelector.glob("a(b", "a(b"));
    }

    @Test
    void manyStarsAreLinearNotCatastrophic() {
        String pattern = "*a".repeat(5_000) + "b";
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () ->
                assertFalse(ServerSelector.glob(pattern, "a".repeat(200))));
        List<Candidate> s = List.of(server("g1", arena("a".repeat(200), null, ArenaInfo.State.WAITING, 0, 8, 1, true)));
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () ->
                assertTrue(ServerSelector.choose(s, TargetKind.GROUP, pattern, 1, Set.of()).isEmpty()));
    }

    @Test
    void sizePatternsStillWorkAndNonSizesFallBackToGlob() {
        List<Candidate> s = List.of(server("g1",
                arena("quad", null, ArenaInfo.State.WAITING, 0, 16, 4, true),
                arena("2x3_map", null, ArenaInfo.State.WAITING, 0, 6, 3, true)));
        assertEquals(List.of("g1/quad"), ids(ServerSelector.choose(s, TargetKind.GROUP, "4x4", 1, Set.of())));
        assertEquals(List.of("g1/2x3_map"), ids(ServerSelector.choose(s, TargetKind.GROUP, "2x3*", 1, Set.of())));
        assertEquals(List.of(), ids(ServerSelector.choose(s, TargetKind.GROUP, "x", 1, Set.of())));
    }

    @Test
    void stateWithNullArenasIsSkipped() {
        ServerState broken = new ServerState("g1", "GAME", false, 0, null);
        broken.arenas = null;
        assertTrue(ServerSelector.choose(List.of(new Candidate("g1", broken, true)), TargetKind.ANY, null, 1, Set.of()).isEmpty());
    }
}
