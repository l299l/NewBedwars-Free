package com.l299l.newbedwars.network;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.common.protocol.MessageType;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.JoinReject;
import com.l299l.newbedwars.common.protocol.payload.JoinTicket;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JoinTicketHandlerTest {
    Messenger messenger;
    Messages messages;
    MockedStatic<Bukkit> bukkit;
    Player a, b;
    IArena duo;
    boolean restarting;
    JoinTicketHandler handler;

    private Player player(String name) {
        Player p = mock(Player.class);
        UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        when(p.getUniqueId()).thenReturn(id);
        when(p.isOnline()).thenReturn(true);
        when(p.getName()).thenReturn(name);
        bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(p);
        return p;
    }

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        messenger = mock(Messenger.class);
        messages = mock(Messages.class);
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getMessages()).thenReturn(messages);
        NewBedwars.plugin = plugin;
        a = player("A");
        b = player("B");
        ArenaRegistry.clear();
        duo = NetTestArenas.arena("duo", GameStatus.waiting, 4, 2, 0, true);
        ArenaRegistry.register("duo", duo);
        restarting = false;
        Plugin owner = mock(Plugin.class);
        when(owner.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        handler = new JoinTicketHandler(owner, messenger, "game1", 10, () -> restarting);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        ArenaRegistry.clear();
        NewBedwars.plugin = null;
    }

    private static PlayerJoinEvent joinEvent(Player p) {
        PlayerJoinEvent e = mock(PlayerJoinEvent.class);
        when(e.getPlayer()).thenReturn(p);
        return e;
    }

    private JoinTicket ticket(Player p, String arena, List<UUID> party) {
        return new JoinTicket("r1", p.getUniqueId(), TargetKind.ARENA, arena, arena, party.isEmpty() ? null : "p1",
                party, System.currentTimeMillis() + 10_000);
    }

    @Test
    void arrivingPlayerWaitsUntilTicketJoins() {
        handler.onJoin(joinEvent(a));
        assertTrue(handler.isAwaiting(a));
        when(duo.join(a)).thenReturn(true);
        handler.onTicket(a, ticket(a, "duo", List.of()));
        verify(duo).join(a);
        assertFalse(handler.isAwaiting(a));
        verifyNoInteractions(messenger);
    }

    @Test
    void bypassPermissionDoesNotWait() {
        when(a.hasPermission(JoinTicketHandler.BYPASS_PERMISSION)).thenReturn(true);
        handler.onJoin(joinEvent(a));
        assertFalse(handler.isAwaiting(a));
    }

    @Test
    void partyJoinsTogetherOnceAllArrived() {
        List<UUID> party = List.of(a.getUniqueId(), b.getUniqueId());
        when(duo.joinParty(anyList())).thenReturn(true);
        handler.onTicket(a, ticket(a, "duo", party));
        verify(duo, never()).joinParty(anyList());
        handler.onTicket(b, ticket(b, "duo", party));
        verify(duo).joinParty(List.of(a, b));
    }

    @Test
    void resentTicketIsIgnored() {
        when(duo.join(a)).thenReturn(true);
        handler.onTicket(a, ticket(a, "duo", List.of()));
        handler.onTicket(a, ticket(a, "duo", List.of()));
        verify(duo, times(1)).join(a);
    }

    @Test
    void fullArenaIsRejectedToTheProxy() {
        when(duo.getActivePlayerCount()).thenReturn(8);
        handler.onTicket(a, ticket(a, "duo", List.of()));
        ArgumentCaptor<NetMessage> msg = ArgumentCaptor.forClass(NetMessage.class);
        verify(messenger).send(eq(a), msg.capture());
        assertEquals(MessageType.JOIN_REJECT, msg.getValue().type());
        JoinReject r = msg.getValue().payload(JoinReject.class);
        assertEquals(JoinReject.Reason.FULL, r.reason);
        assertEquals("r1", r.requestId);
        assertEquals("duo", r.arena);
        verify(messages).send(a, "NetworkJoinRejected");
        assertTrue(handler.isAwaiting(a), "the timeout still sends them to the lobby if the proxy does not");
    }

    @Test
    void failedJoinAndRestartingAreRejected() {
        when(duo.join(a)).thenReturn(false);
        handler.onTicket(a, ticket(a, "duo", List.of()));
        restarting = true;
        handler.onTicket(b, new JoinTicket("r2", b.getUniqueId(), TargetKind.ANY, null, null, null, List.of(), 0));
        ArgumentCaptor<NetMessage> msg = ArgumentCaptor.forClass(NetMessage.class);
        verify(messenger, times(2)).send(any(Player.class), msg.capture());
        assertEquals(JoinReject.Reason.FULL, msg.getAllValues().get(0).payload(JoinReject.class).reason);
        assertEquals(JoinReject.Reason.NOT_JOINABLE, msg.getAllValues().get(1).payload(JoinReject.class).reason);
    }

    @Test
    void expiredTicketIsRejected() {
        handler.onTicket(a, new JoinTicket("r3", a.getUniqueId(), TargetKind.ARENA, "duo", "duo", null, List.of(), 1));
        ArgumentCaptor<NetMessage> msg = ArgumentCaptor.forClass(NetMessage.class);
        verify(messenger).send(eq(a), msg.capture());
        assertEquals(JoinReject.Reason.EXPIRED, msg.getValue().payload(JoinReject.class).reason);
        verify(duo, never()).join(any());
    }

    @Test
    void rejoinTicketRejoinsOrRejects() {
        IArena running = NetTestArenas.arena("run", GameStatus.playing, 2, 1, 1, true);
        ArenaRegistry.register("run", running);
        when(running.isPlayerInArena(a)).thenReturn(true);
        when(running.rejoin(a)).thenReturn(true);
        handler.onTicket(a, new JoinTicket("r9", a.getUniqueId(), TargetKind.REJOIN, null, "run", null, List.of(), 0));
        verify(running).rejoin(a);
        verify(messages).send(a, "Rejoined");
        verifyNoInteractions(messenger);

        handler.onTicket(b, new JoinTicket("r10", b.getUniqueId(), TargetKind.REJOIN, null, "run", null, List.of(), 0));
        verify(running, never()).rejoin(b);
        verify(messages).send(b, "RejoinFailed");
        ArgumentCaptor<NetMessage> msg = ArgumentCaptor.forClass(NetMessage.class);
        verify(messenger).send(eq(b), msg.capture());
        assertEquals(JoinReject.Reason.OTHER, msg.getValue().payload(JoinReject.class).reason);
    }

    @Test
    void spectateTicketWatchesOrRejects() {
        IArena running = NetTestArenas.arena("run", GameStatus.playing, 2, 1, 2, true);
        ArenaRegistry.register("run", running);
        when(running.joinAsSpectator(a)).thenReturn(true);
        handler.onTicket(a, new JoinTicket("s1", a.getUniqueId(), TargetKind.SPECTATE, "run", "run", null, List.of(), 0));
        verify(running).joinAsSpectator(a);
        assertFalse(handler.isAwaiting(a));
        verifyNoInteractions(messenger);

        handler.onTicket(b, new JoinTicket("s2", b.getUniqueId(), TargetKind.SPECTATE, "run", "run", null, List.of(), 0));
        verify(messages).send(b, "SpectateFailed");
        ArgumentCaptor<NetMessage> msg = ArgumentCaptor.forClass(NetMessage.class);
        verify(messenger).send(eq(b), msg.capture());
        assertEquals(JoinReject.Reason.NOT_JOINABLE, msg.getValue().payload(JoinReject.class).reason);
    }

    @Test
    void holdKeepsAPlayerWhoIsBeingMoved() {
        handler.hold(a);
        assertTrue(handler.isAwaiting(a));
    }
}
