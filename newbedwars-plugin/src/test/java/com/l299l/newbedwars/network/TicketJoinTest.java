package com.l299l.newbedwars.network;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.common.protocol.payload.JoinReject;
import com.l299l.newbedwars.common.protocol.payload.JoinTicket;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.l299l.newbedwars.network.NetTestArenas.arena;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TicketJoinTest {
    IArena solo1, solo2, duo, running;

    @BeforeEach
    void setUp() {
        ArenaRegistry.clear();
        solo1 = arena("solo_1", GameStatus.waiting, 8, 1, 1, false);
        solo2 = arena("solo_2", GameStatus.waiting, 8, 1, 5, false);
        duo = arena("duo_1", GameStatus.starting, 4, 2, 2, true);
        running = arena("solo_3", GameStatus.playing, 8, 1, 7, false);
        for (IArena a : List.of(solo1, solo2, duo, running)) ArenaRegistry.register(a.getArenaName(), a);
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
    }

    private static JoinTicket ticket(TargetKind kind, String target, String arena) {
        return new JoinTicket("r1", UUID.randomUUID(), kind, target, arena, null, List.of(), 0);
    }

    private IArena resolve(JoinTicket t, int n) {
        return TicketJoin.resolve(t, ArenaRegistry.all(), ArenaRegistry::find, n);
    }

    @Test
    void pickedArenaWins() {
        assertSame(solo1, resolve(ticket(TargetKind.GROUP, "solo_*", "SOLO_1"), 1));
        assertNull(resolve(ticket(TargetKind.ANY, null, "gone"), 1));
    }

    @Test
    void blindTicketsResolveLocally() {
        assertSame(solo2, resolve(ticket(TargetKind.GROUP, "solo_*", null), 1), "fullest with room");
        assertSame(duo, resolve(ticket(TargetKind.GROUP, "4x2", null), 1));
        assertSame(duo, resolve(ticket(TargetKind.ARENA, "duo_1", null), 1));
        assertSame(solo2, resolve(ticket(TargetKind.ANY, null, null), 1));
        assertSame(duo, resolve(ticket(TargetKind.ANY, null, null), 2), "only duo takes a party of 2");
        assertNull(resolve(ticket(TargetKind.GROUP, "nothing", null), 1));
    }

    @Test
    void checkReasons() {
        assertNull(TicketJoin.check(solo1, 1));
        assertEquals(JoinReject.Reason.NO_ARENA, TicketJoin.check(null, 1));
        assertEquals(JoinReject.Reason.NOT_JOINABLE, TicketJoin.check(running, 1));
        assertEquals(JoinReject.Reason.PARTY_NOT_ALLOWED, TicketJoin.check(solo1, 2));
        assertEquals(JoinReject.Reason.PARTY_NOT_ALLOWED, TicketJoin.check(duo, 3));
        when(duo.getActivePlayerCount()).thenReturn(7);
        assertEquals(JoinReject.Reason.FULL, TicketJoin.check(duo, 2));
        when(solo1.isEnabled()).thenReturn(false);
        assertEquals(JoinReject.Reason.NOT_JOINABLE, TicketJoin.check(solo1, 1));
    }
}
