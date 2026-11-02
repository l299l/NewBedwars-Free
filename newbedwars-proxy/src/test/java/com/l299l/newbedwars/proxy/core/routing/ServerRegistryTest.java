package com.l299l.newbedwars.proxy.core.routing;

import com.l299l.newbedwars.common.protocol.Limits;
import com.l299l.newbedwars.common.protocol.StateFit;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.common.protocol.payload.NetworkState;
import com.l299l.newbedwars.common.protocol.payload.RejoinSlot;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServerRegistryTest {
    static final long STALE = 15_000;
    ServerRegistry reg;

    @BeforeEach
    void setUp() {
        reg = new ServerRegistry(List.of("game1", "game2"), List.of("lobby"), STALE);
    }

    private static ArenaInfo arena(String name, int players) {
        return new ArenaInfo(name, null, ArenaInfo.State.WAITING, players, 8, 1, 8, true);
    }

    private static ServerState state(String id, long time, ArenaInfo... arenas) {
        return new ServerState(id, "GAME", false, time, new ArrayList<>(Arrays.asList(arenas)));
    }

    private static ArenaInfo first(NetworkState s, String server) {
        return s.servers.stream().filter(x -> x.serverId.equals(server)).findFirst().orElseThrow().arenas.get(0);
    }

    @Test
    void staleBoundaryIsInclusive() {
        reg.update("game1", state("game1", 0, arena("a", 0)), 1000);
        assertTrue(reg.isFresh("game1", 1000 + STALE));
        assertFalse(reg.isFresh("game1", 1000 + STALE + 1));
        assertFalse(reg.isFresh("game2", 1000), "never heard from");
    }

    @Test
    void nullArenasAndNamesAreDropped() {
        reg.update("game1", new ServerState("game1", "GAME", false, 0, null), 0);
        assertTrue(reg.snapshot(0).servers.get(0).arenas.isEmpty());
        reg.update("game1", state("game1", 0, null, arena(null, 1), arena("ok", 2)), 0);
        List<ArenaInfo> arenas = reg.snapshot(0).servers.get(0).arenas;
        assertEquals(1, arenas.size());
        assertEquals("ok", arenas.get(0).name);
        reg.update(null, state("x", 0), 0);
        reg.update("game1", null, 0);
        assertEquals(1, reg.snapshot(0).servers.size());
    }

    @Test
    void callersAndSnapshotsAreNotMutated() {
        ArenaInfo mine = arena("a", 1);
        mine.rejoin.add(new RejoinSlot(UUID.randomUUID(), 5));
        ServerState sent = state("other", 0, mine);
        reg.update("game1", sent, 0);
        assertEquals("other", sent.serverId, "the caller's state keeps its own id");
        reg.reserve("game1", "a", 3);
        assertEquals(1, mine.players);
        NetworkState snap = reg.snapshot(0);
        first(snap, "game1").players = 99;
        first(snap, "game1").rejoin.clear();
        assertEquals(4, first(reg.snapshot(0), "game1").players);
        assertEquals(1, first(reg.snapshot(0), "game1").rejoin.size());
    }

    @Test
    void reserveForUnknownServerOrArenaIsANoOp() {
        reg.reserve("game1", "a", 1);
        reg.reserve(null, "a", 1);
        assertFalse(reg.takeDirty());
        reg.update("game1", state("game1", 0, arena("a", 0)), 0);
        reg.takeDirty();
        reg.reserve("game1", "nope", 1);
        reg.reserve("game1", null, 1);
        assertFalse(reg.takeDirty());
        reg.reserve("game1", "A", 2);
        assertTrue(reg.takeDirty());
        assertEquals(2, first(reg.snapshot(0), "game1").players);
    }

    @Test
    void setGroupsRelabelsKnownArenas() {
        ArenaInfo a = arena("a", 0);
        a.group = "own";
        reg.update("game1", state("game1", 0, a), 0);
        assertEquals("own", first(reg.snapshot(0), "game1").group);
        reg.takeDirty();
        reg.setGroups(new NetworkGroups(1, List.of(new NetworkGroups.Group("duels", "PAPER", List.of("a")))));
        assertTrue(reg.takeDirty());
        assertEquals("duels", first(reg.snapshot(0), "game1").group);
        reg.update("game1", state("game1", 1, arena("a", 0)), 1);
        assertEquals("duels", first(reg.snapshot(1), "game1").group);
    }

    @Test
    void snapshotExcludesStaleAndIsSorted() {
        reg.update("game2", state("game2", 0, arena("b", 0)), 0);
        reg.update("game1", state("game1", 0, arena("a", 0)), 0);
        reg = new ServerRegistry(List.of(), List.of("lobby"), STALE);
        reg.update("game2", state("game2", 0, arena("b", 0)), 0);
        reg.update("game1", state("game1", 0, arena("a", 0)), 0);
        reg.update("Alpha", state("Alpha", 0, arena("c", 0)), 0);
        reg.update("old", state("old", 0, arena("d", 0)), -STALE - 1);
        List<String> ids = reg.snapshot(0).servers.stream().map(s -> s.serverId).toList();
        assertEquals(List.of("Alpha", "game1", "game2"), ids);
    }

    @Test
    void takeDirtyIsTrueOnceAfterAChange() {
        assertFalse(reg.takeDirty());
        reg.update("game1", state("game1", 0, arena("a", 0)), 0);
        assertTrue(reg.takeDirty());
        assertFalse(reg.takeDirty());
        reg.markDirty();
        assertTrue(reg.takeDirty());
    }

    @Test
    void lobbiesAreExcluded() {
        reg.update("lobby", state("lobby", 0, arena("a", 0)), 0);
        reg.mergeFromDatabase(List.of(state("lobby", 5, arena("a", 0))), 0);
        assertTrue(reg.snapshot(0).servers.isEmpty());
        assertFalse(reg.gameServers().contains("lobby"));
        assertFalse(reg.isGame("lobby"));
        assertTrue(reg.isLobby("lobby"));
    }

    @Test
    void databaseRowsWithoutServerIdAreSkipped() {
        ServerState noId = state(null, 5, arena("a", 0));
        reg.mergeFromDatabase(java.util.Arrays.asList(null, noId, state("game1", 5, arena("a", 0))), 100);
        assertEquals(1, reg.snapshot(100).servers.size());
    }

    @Test
    void databaseRowIsFreshFromTheProxyClockWhenItsHeartbeatChanges() {
        // the game server's clock is far ahead of the proxy's: only changes matter
        reg.mergeFromDatabase(List.of(state("game1", 9_000_000, arena("a", 1))), 1000);
        assertTrue(reg.isFresh("game1", 1000));
        assertFalse(reg.isFresh("game1", 1000 + STALE + 1), "same heartbeat: no sign of life");
        reg.mergeFromDatabase(List.of(state("game1", 9_000_000, arena("a", 5))), 1000 + STALE + 1);
        assertFalse(reg.isFresh("game1", 1000 + STALE + 1));
        reg.mergeFromDatabase(List.of(state("game1", 9_005_000, arena("a", 5))), 20_000);
        assertTrue(reg.isFresh("game1", 20_000));
        assertEquals(5, first(reg.snapshot(20_000), "game1").players);
    }

    @Test
    void freshPluginMessageStateIsNotReplacedByADatabaseRow() {
        reg.update("game1", state("game1", 0, arena("a", 1)), 1000);
        reg.mergeFromDatabase(List.of(state("game1", 777, arena("a", 7))), 2000);
        assertEquals(1, first(reg.snapshot(2000), "game1").players);
        // once the message state is stale, a changed heartbeat takes over
        reg.mergeFromDatabase(List.of(state("game1", 888, arena("a", 7))), 1000 + STALE + 1);
        assertEquals(7, first(reg.snapshot(1000 + STALE + 1), "game1").players);
    }

    @Test
    void reservationsSurviveRepeatedDatabaseRows() {
        reg.mergeFromDatabase(List.of(state("game1", 10, arena("a", 1))), 1000);
        reg.reserve("game1", "a", 2);
        reg.mergeFromDatabase(List.of(state("game1", 10, arena("a", 1))), 2000);
        assertEquals(3, first(reg.snapshot(2000), "game1").players);
    }

    @Test
    void databaseStateIsCopied() {
        ServerState row = state("game1", 10, arena("a", 1));
        reg.mergeFromDatabase(List.of(row), 0);
        reg.reserve("game1", "a", 1);
        assertEquals(1, row.arenas.get(0).players);
    }

    private static ServerState bigState(String id, int arenas, int rejoinPerArena) {
        List<ArenaInfo> list = new ArrayList<>();
        for (int i = 0; i < arenas; i++) {
            ArenaInfo a = new ArenaInfo("arena-with-a-long-name-" + i, "group", i % 2 == 0 ? ArenaInfo.State.WAITING
                    : ArenaInfo.State.PLAYING, i, 16, 2, 8, true);
            for (int j = 0; j < rejoinPerArena; j++) a.rejoin.add(new RejoinSlot(UUID.randomUUID(), 1));
            list.add(a);
        }
        return new ServerState(id, "GAME", false, 0, list);
    }

    @Test
    void stateFromUnconfiguredServersIsIgnoredOnceLoggedAndRowsToo() {
        List<String> log = new ArrayList<>();
        reg.setLogger(log::add);
        reg.update("evil", state("evil", 0, arena("a", 0)), 0);
        reg.update("evil", state("evil", 0, arena("a", 0)), 0);
        reg.mergeFromDatabase(List.of(state("evil", 5, arena("a", 0)), state("evil2", 5, arena("a", 0))), 0);
        assertFalse(reg.isGame("evil"));
        assertFalse(reg.isGame("evil2"));
        assertTrue(reg.snapshot(0).servers.isEmpty());
        assertEquals(2, log.size(), "each rejected id once");
        reg.update("game1", state("game1", 0, arena("a", 0)), 0);
        assertEquals(1, reg.snapshot(0).servers.size());
    }

    @Test
    void withoutConfiguredServersAnyServerIsLearnedUpToTheLimit() {
        reg = new ServerRegistry(List.of(), List.of("lobby"), STALE);
        for (int i = 0; i < Limits.MAX_SERVERS + 20; i++) reg.update("s" + i, state("s" + i, 0, arena("a", 0)), 0);
        assertEquals(Limits.MAX_SERVERS, reg.gameServers().size());
        assertTrue(reg.isGame("s0"));
        assertFalse(reg.isGame("s" + (Limits.MAX_SERVERS + 5)));
        reg.update("x".repeat(Limits.MAX_NAME_LENGTH + 1), state("x", 0, arena("a", 0)), 0);
        assertEquals(Limits.MAX_SERVERS, reg.gameServers().size());
    }

    @Test
    void longSilentServersArePruned() {
        reg.update("game1", state("game1", 0, arena("a", 0)), 0);
        reg.prune(STALE * 10);
        assertTrue(reg.candidates(0).stream().anyMatch(c -> c.serverId.equals("game1") && c.state != null));
        reg.prune(STALE * 10 + 1);
        assertTrue(reg.candidates(0).stream().noneMatch(c -> c.state != null));
        assertTrue(reg.takeDirty());
    }

    @Test
    void prunedServerIsNotRevivedByItsOldDatabaseRow() {
        reg.mergeFromDatabase(List.of(state("game1", 77, arena("a", 0))), 0);
        reg.prune(STALE * 10 + 1);
        reg.mergeFromDatabase(List.of(state("game1", 77, arena("a", 0))), STALE * 10 + 2);
        assertFalse(reg.isFresh("game1", STALE * 10 + 2));
        reg.mergeFromDatabase(List.of(state("game1", 78, arena("a", 0))), STALE * 10 + 3);
        assertTrue(reg.isFresh("game1", STALE * 10 + 3));
    }

    @Test
    void releaseGivesPlayersBackOnlyWhileTheReservedStateIsCurrent() {
        reg.update("game1", state("game1", 0, arena("a", 1)), 0);
        ServerRegistry.Reservation r = reg.reserve("game1", "a", 3);
        reg.release(r, 2);
        assertEquals(2, first(reg.snapshot(0), "game1").players);
        reg.release(r, 5);
        assertEquals(1, first(reg.snapshot(0), "game1").players, "only what is still reserved");
        ServerRegistry.Reservation r2 = reg.reserve("game1", "a", 1);
        reg.update("game1", state("game1", 1, arena("a", 2)), 1);
        reg.release(r2, 1);
        assertEquals(2, first(reg.snapshot(1), "game1").players);
        reg.release(null, 1);
        assertNull(reg.reserve("game1", "nope", 1));
    }

    @Test
    void oversizedSnapshotDropsRejoinDataFirst() {
        List<String> log = new ArrayList<>();
        reg.setLogger(log::add);
        reg.update("game1", bigState("game1", 20, 64), 0);
        NetworkState snap = reg.snapshot(0);
        assertTrue(StateFit.size(snap) <= Limits.MAX_PAYLOAD_BYTES);
        assertEquals(20, snap.servers.get(0).arenas.size());
        assertTrue(snap.servers.get(0).arenas.stream().allMatch(a -> a.rejoin.isEmpty()));
        reg.snapshot(0);
        assertEquals(1, log.size(), "logged once");
        assertEquals(64, reg.candidates(0).get(0).state.arenas.get(0).rejoin.size(), "the stored state keeps its data");
    }

    @Test
    void oversizedSnapshotTruncatesLeastUsefulArenas() {
        reg = new ServerRegistry(List.of(), List.of("lobby"), STALE);
        for (int i = 0; i < 20; i++) reg.update("g" + i, bigState("g" + i, 64, 0), 0);
        NetworkState snap = reg.snapshot(0);
        assertTrue(StateFit.size(snap) <= Limits.MAX_PAYLOAD_BYTES);
        long kept = snap.servers.stream().mapToLong(s -> s.arenas.size()).sum();
        assertTrue(kept > 0 && kept < 20 * 64);
        long running = snap.servers.stream().flatMap(s -> s.arenas.stream()).filter(a -> !a.isJoinable()).count();
        long joinable = kept - running;
        assertTrue(joinable > running, "running arenas go first");
    }
}
