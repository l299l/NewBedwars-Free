package com.l299l.newbedwars.network;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.l299l.newbedwars.network.NetTestArenas.arena;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ArenaStateSnapshotTest {

    @Test
    void buildsSortedArenaListWithGroupsAndStates() {
        IArena b = arena("b", GameStatus.playing, 4, 2, 6, true);
        IArena a = arena("A", GameStatus.starting, 8, 1, 3, false);
        IArena off = arena("c", GameStatus.waiting, 2, 1, 0, true);
        when(off.isEnabled()).thenReturn(false);
        ServerState s = ArenaStateSnapshot.build("game1", List.of(b, a, off), x -> x == a ? "solo" : null, true, 42L);
        assertEquals("game1", s.serverId);
        assertEquals("GAME", s.mode);
        assertTrue(s.restarting);
        assertEquals(42L, s.time);
        assertEquals(List.of("A", "b", "c"), s.arenas.stream().map(i -> i.name).toList());
        ArenaInfo ia = s.arenas.get(0);
        assertEquals("solo", ia.group);
        assertEquals(ArenaInfo.State.STARTING, ia.state);
        assertEquals(3, ia.players);
        assertEquals(8, ia.maxPlayers);
        assertEquals(8, ia.teams);
        assertFalse(ia.allowParties);
        ArenaInfo ib = s.arenas.get(1);
        assertNull(ib.group);
        assertEquals(ArenaInfo.State.PLAYING, ib.state);
        assertEquals(2, ib.maxInTeam);
        assertTrue(ib.allowParties);
        assertEquals(ArenaInfo.State.DISABLED, s.arenas.get(2).state);
    }

    @Test
    void everyStatusMaps() {
        assertEquals(ArenaInfo.State.WAITING, ArenaStateSnapshot.state(arena("x", GameStatus.waiting, 1, 1, 0, true)));
        assertEquals(ArenaInfo.State.ENDING, ArenaStateSnapshot.state(arena("x", GameStatus.ending, 1, 1, 0, true)));
        assertEquals(ArenaInfo.State.RESTARTING, ArenaStateSnapshot.state(arena("x", GameStatus.restarting, 1, 1, 0, true)));
    }

    @Test
    void rejoinSlotsArePublished() {
        IArena a = arena("x", GameStatus.playing, 2, 1, 1, true);
        java.util.UUID id = java.util.UUID.randomUUID();
        when(a.getRejoinSlots()).thenReturn(java.util.Map.of(id, 99L));
        ArenaInfo i = ArenaStateSnapshot.info(a, null);
        assertEquals(1, i.rejoin.size());
        assertEquals(id, i.rejoin.get(0).player);
        assertEquals(99L, i.rejoin.get(0).expiresAt);
    }
}
