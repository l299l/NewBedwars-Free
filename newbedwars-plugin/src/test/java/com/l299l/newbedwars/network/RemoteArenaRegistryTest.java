package com.l299l.newbedwars.network;

import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.NetworkState;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RemoteArenaRegistryTest {

    private static ArenaInfo info(String name, String group, ArenaInfo.State state, int players) {
        return new ArenaInfo(name, group, state, players, 8, 1, 8, true);
    }

    private static NetworkState state(ArenaInfo... g1) {
        return new NetworkState(List.of(
                new ServerState("game1", "GAME", false, 0, List.of(g1)),
                new ServerState("game2", "GAME", false, 0, List.of(info("a", "solo", ArenaInfo.State.WAITING, 0)))));
    }

    @Test
    void updateReportsChangesOnly() {
        RemoteArenaRegistry r = new RemoteArenaRegistry();
        assertTrue(r.update(state(info("b", "solo", ArenaInfo.State.WAITING, 1))));
        assertFalse(r.update(state(info("b", "solo", ArenaInfo.State.WAITING, 1))));
        assertTrue(r.update(state(info("b", "solo", ArenaInfo.State.WAITING, 2))));
        assertTrue(r.update(state(info("b", "solo", ArenaInfo.State.PLAYING, 2))));
        assertEquals(List.of("a", "b"), r.all().stream().map(RemoteArena::getName).toList());
        assertTrue(r.update(null));
        assertTrue(r.all().isEmpty());
    }

    @Test
    void lookupsAndBest() {
        RemoteArenaRegistry r = new RemoteArenaRegistry();
        r.update(state(info("b", "solo", ArenaInfo.State.WAITING, 3), info("quad", null, ArenaInfo.State.WAITING, 0)));
        assertEquals("game1", r.byName("B").getServer());
        assertNull(r.byName("nope"));
        assertEquals(2, r.matching(TargetKind.GROUP, "solo").size());
        assertEquals(1, r.matching(TargetKind.ARENA, "quad").size());
        assertEquals(3, r.matching(TargetKind.ANY, null).size());
        assertEquals("b", RemoteArenaRegistry.best(r.matching(TargetKind.GROUP, "solo"), 1).getName());
        assertNull(RemoteArenaRegistry.best(r.matching(TargetKind.GROUP, "solo"), 9));
    }

    @Test
    void apiView() {
        RemoteArena a = new RemoteArena("game1", info("x", "solo", ArenaInfo.State.ENDING, 4));
        assertEquals("game1", a.getServer());
        assertEquals(Optional.of("solo"), a.getGroup());
        assertEquals(Optional.of(ArenaState.ENDING), a.getState());
        assertEquals(4, a.getPlayerCount());
        assertEquals(8, a.getMaxPlayers());
        assertEquals(Optional.empty(), new RemoteArena("g", info("y", null, ArenaInfo.State.DISABLED, 0)).getState());
    }

    @Test
    void totals() {
        var t = NetworkManager.totals(List.of(
                new RemoteArena("g", info("a", null, ArenaInfo.State.WAITING, 2)),
                new RemoteArena("g", info("b", null, ArenaInfo.State.PLAYING, 6)),
                new RemoteArena("g", info("c", null, ArenaInfo.State.DISABLED, 5))));
        assertEquals(2, t.get("waiting"));
        assertEquals(6, t.get("playing"));
        assertEquals(1, t.get("free"));
        assertEquals(8, t.get("players"));
        assertEquals(16, t.get("max"));
    }

    @Test
    void findRejoinAndSlotChangesCount() {
        java.util.UUID me = java.util.UUID.randomUUID();
        RemoteArenaRegistry r = new RemoteArenaRegistry();
        ArenaInfo running = info("b", "solo", ArenaInfo.State.PLAYING, 3);
        r.update(state(running));
        assertNull(r.findRejoin(me, 0));
        ArenaInfo withSlot = info("b", "solo", ArenaInfo.State.PLAYING, 3);
        withSlot.rejoin.add(new com.l299l.newbedwars.common.protocol.payload.RejoinSlot(me, 1000));
        assertTrue(r.update(state(withSlot)), "a new rejoin slot is a change");
        assertEquals("b", r.findRejoin(me, 500).getName());
        assertNull(r.findRejoin(me, 1000));
        assertEquals("1:05", RejoinNotifier.format(65));
    }

    @Test
    void groupEntriesResolveToServers() {
        RemoteArenaRegistry r = new RemoteArenaRegistry();
        r.update(state(info("castle", null, ArenaInfo.State.WAITING, 0), info("a", null, ArenaInfo.State.WAITING, 0)));
        assertEquals(List.of("game1", "game2"), r.resolveEntry("A").stream().map(RemoteArena::getServer).toList());
        assertEquals(List.of("game2"), r.resolveEntry("GAME2:a").stream().map(RemoteArena::getServer).toList());
        assertTrue(r.resolveEntry("game2:castle").isEmpty());
        assertTrue(r.resolveEntry(" ").isEmpty());
        var m = r.members("a");
        assertEquals("game1:a", m.get(0).name());
        assertEquals(8, m.get(0).teams());
        assertEquals(1, m.get(0).maxInTeam());
    }
}
