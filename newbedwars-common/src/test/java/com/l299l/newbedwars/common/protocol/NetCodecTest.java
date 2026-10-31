package com.l299l.newbedwars.common.protocol;

import com.l299l.newbedwars.common.protocol.payload.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NetCodecTest {
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    private static NetMessage roundTrip(Object payload) {
        NetMessage msg = NetMessage.of("game1", payload);
        NetMessage back = NetCodec.decode(NetCodec.encode(msg));
        assertNotNull(back);
        assertEquals(msg.type(), back.type());
        assertEquals("game1", back.from());
        assertEquals(msg.id(), back.id());
        assertEquals(NetMessage.VERSION, back.version());
        return back;
    }

    @Test
    void hello() {
        Hello h = roundTrip(new Hello("game1", "GAME", "2.5")).payload(Hello.class);
        assertEquals("game1", h.serverId);
        assertEquals("GAME", h.mode);
        assertEquals("2.5", h.pluginVersion);
    }

    @Test
    void serverStateAndNetworkState() {
        ArenaInfo a = new ArenaInfo("solo_1", "solo", ArenaInfo.State.STARTING, 3, 8, 1, 8, false);
        a.rejoin.add(new RejoinSlot(A, 777L));
        ServerState s = new ServerState("game1", "GAME", true, 123L, List.of(a));
        ServerState back = roundTrip(s).payload(ServerState.class);
        assertTrue(back.restarting);
        assertEquals(123L, back.time);
        ArenaInfo ba = back.arenas.get(0);
        assertEquals("solo_1", ba.name);
        assertEquals("solo", ba.group);
        assertEquals(ArenaInfo.State.STARTING, ba.state);
        assertEquals(3, ba.players);
        assertEquals(8, ba.maxPlayers);
        assertEquals(1, ba.maxInTeam);
        assertEquals(8, ba.teams);
        assertFalse(ba.allowParties);
        assertEquals(A, ba.rejoin.get(0).player);
        assertEquals(777L, ba.rejoin.get(0).expiresAt);
        assertNotNull(ba.rejoinSlot(A, 700L));
        assertNull(ba.rejoinSlot(A, 778L));

        NetworkState ns = roundTrip(new NetworkState(List.of(s))).payload(NetworkState.class);
        assertEquals("game1", ns.servers.get(0).serverId);
        assertEquals("solo_1", ns.servers.get(0).arenas.get(0).name);
    }

    @Test
    void joinMessages() {
        JoinRequest r = roundTrip(new JoinRequest(A, TargetKind.GROUP, "solo")).payload(JoinRequest.class);
        assertEquals(A, r.player);
        assertEquals(TargetKind.GROUP, r.kind);
        assertEquals("solo", r.target);

        JoinTicket t = roundTrip(new JoinTicket("r1", A, TargetKind.ARENA, "x", "x", "p1", List.of(A, B), 99L))
                .payload(JoinTicket.class);
        assertEquals("r1", t.requestId);
        assertEquals(A, t.player);
        assertEquals("x", t.arena);
        assertEquals("p1", t.partyId);
        assertEquals(List.of(A, B), t.party);
        assertEquals(99L, t.expiresAt);

        JoinReject j = roundTrip(new JoinReject("r1", A, "x", JoinReject.Reason.FULL)).payload(JoinReject.class);
        assertEquals(JoinReject.Reason.FULL, j.reason);
        assertEquals("r1", j.requestId);
        assertEquals("x", j.arena);
    }

    @Test
    void partyAndLobby() {
        PartySync p = roundTrip(new PartySync("p1", A, List.of(new PartySync.Member(A, "Leo"), new PartySync.Member(B, "Bot")),
                false, List.of(B))).payload(PartySync.class);
        assertEquals(A, p.admin);
        assertEquals("Bot", p.members.get(1).name);
        assertEquals(List.of(B), p.removed);
        assertFalse(p.disbanded);

        SendToLobby s = roundTrip(new SendToLobby(List.of(A))).payload(SendToLobby.class);
        assertEquals(List.of(A), s.players);
    }

    @Test
    void groups() {
        NetworkGroups g = roundTrip(new NetworkGroups(42L, List.of(
                new NetworkGroups.Group("solo", "PAPER", List.of("castle", "game2:ruins"))))).payload(NetworkGroups.class);
        assertEquals(42L, g.revision);
        assertEquals("solo", g.groups.get(0).name);
        assertEquals("PAPER", g.groups.get(0).displayItem);
        assertEquals(List.of("castle", "game2:ruins"), g.groups.get(0).arenas);
    }

    @Test
    void groupEntries() {
        assertNull(NetworkGroups.serverOf("castle"));
        assertEquals("game2", NetworkGroups.serverOf("game2:ruins"));
        assertEquals("ruins", NetworkGroups.arenaOf("game2:ruins"));
        assertTrue(NetworkGroups.covers("castle", "game1", "Castle"));
        assertTrue(NetworkGroups.covers("GAME1:castle", "game1", "castle"));
        assertFalse(NetworkGroups.covers("game2:castle", "game1", "castle"));
        assertTrue(NetworkGroups.overlaps("castle", "game1:castle"));
        assertTrue(NetworkGroups.overlaps("game1:castle", "Game1:Castle"));
        assertFalse(NetworkGroups.overlaps("game1:castle", "game2:castle"));
        assertFalse(NetworkGroups.overlaps("castle", "ruins"));
    }

    @Test
    void everyTypeIsCovered() {
        for (MessageType t : MessageType.values()) {
            assertNotNull(t.payloadClass());
        }
        assertEquals(9, MessageType.values().length, "add a round-trip test for the new message type");
    }

    @Test
    void badInputGivesNull() {
        assertNull(NetCodec.decode(null));
        assertNull(NetCodec.decode(new byte[0]));
        assertNull(NetCodec.fromJson("not json {"));
        assertNull(NetCodec.fromJson("[1,2]"));
        assertNull(NetCodec.fromJson("{\"v\":2,\"type\":\"HELLO\",\"payload\":{}}"));
        assertNull(NetCodec.fromJson("{\"v\":1,\"type\":\"NOPE\",\"payload\":{}}"));
        assertNull(NetCodec.fromJson("{\"v\":1,\"type\":\"HELLO\"}"));
        assertNull(NetCodec.fromJson("{\"type\":\"HELLO\",\"payload\":{}}"));
        assertNull(NetCodec.decode("{\"v\":1,\"type\":\"JOIN_REQUEST\",\"payload\":{\"player\":\"no-uuid\"}}"
                .getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void wrongPayloadClassIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new NetMessage(MessageType.HELLO, "x", new SendToLobby()));
        assertThrows(IllegalArgumentException.class, () -> NetMessage.of("x", "a string"));
    }
}
