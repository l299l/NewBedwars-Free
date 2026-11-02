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

    private static <T> T decode(String type, String payload, Class<T> cls) {
        NetMessage m = NetCodec.fromJson("{\"v\":1,\"type\":\"" + type + "\",\"from\":\"x\",\"payload\":" + payload + "}");
        assertNotNull(m);
        return m.payload(cls);
    }

    @Test
    void missingAndNullListsBecomeEmpty() {
        ServerState s = decode("SERVER_STATE", "{\"serverId\":\"g\"}", ServerState.class);
        assertNotNull(s.arenas);
        assertTrue(s.arenas.isEmpty());
        assertTrue(decode("SERVER_STATE", "{\"arenas\":null}", ServerState.class).arenas.isEmpty());
        assertTrue(decode("NETWORK_STATE", "{}", NetworkState.class).servers.isEmpty());
        assertTrue(decode("NETWORK_STATE", "{\"servers\":null}", NetworkState.class).servers.isEmpty());
        assertTrue(decode("SEND_TO_LOBBY", "{\"players\":null}", SendToLobby.class).players.isEmpty());
        JoinRequest r = decode("JOIN_REQUEST", "{\"players\":null}", JoinRequest.class);
        assertTrue(r.players.isEmpty());
        PartySync p = decode("PARTY_SYNC", "{\"members\":null,\"removed\":null}", PartySync.class);
        assertTrue(p.members.isEmpty());
        assertTrue(p.removed.isEmpty());
        assertTrue(decode("JOIN_TICKET", "{}", JoinTicket.class).party.isEmpty());
        assertTrue(decode("GROUPS", "{\"groups\":null}", NetworkGroups.class).groups.isEmpty());
    }

    @Test
    void nullElementsAndUnnamedEntriesAreDropped() {
        ServerState s = decode("SERVER_STATE", "{\"arenas\":[null,{},{\"name\":\"a\",\"rejoin\":null},"
                + "{\"name\":\"b\",\"rejoin\":[null,{\"player\":\"" + A + "\"}]}]}", ServerState.class);
        assertEquals(2, s.arenas.size());
        assertTrue(s.arenas.get(0).rejoin.isEmpty());
        assertEquals(1, s.arenas.get(1).rejoin.size());

        NetworkState n = decode("NETWORK_STATE", "{\"servers\":[null,{\"arenas\":[{\"name\":\"x\"}]},"
                + "{\"serverId\":\"g\",\"arenas\":[null,{\"name\":\"y\"}]}]}", NetworkState.class);
        assertEquals(1, n.servers.size());
        assertEquals(1, n.servers.get(0).arenas.size());

        SendToLobby l = decode("SEND_TO_LOBBY", "{\"players\":[null,\"" + A + "\"]}", SendToLobby.class);
        assertEquals(List.of(A), l.players);
        PartySync p = decode("PARTY_SYNC", "{\"members\":[null,{\"name\":\"nouuid\"},{\"uuid\":\"" + B
                + "\"}],\"removed\":[null]}", PartySync.class);
        assertEquals(1, p.members.size());
        assertTrue(p.removed.isEmpty());
        NetworkGroups g = decode("GROUPS", "{\"groups\":[null,{\"arenas\":[\"a\"]},{\"name\":\"s\",\"arenas\":[null,\"a\"]}]}",
                NetworkGroups.class);
        assertEquals(1, g.groups.size());
        assertEquals(List.of("a"), g.groups.get(0).arenas);
    }

    @Test
    void payloadFromJsonNormalizesToo() {
        ServerState s = NetCodec.payloadFromJson("{\"arenas\":[null,{\"name\":\"a\"}]}", ServerState.class);
        assertEquals(1, s.arenas.size());
        assertTrue(s.arenas.get(0).rejoin.isEmpty());
        assertTrue(NetCodec.payloadFromJson("{}", ServerState.class).arenas.isEmpty());
        assertNull(NetCodec.payloadFromJson("garbage", ServerState.class));
        assertNull(NetCodec.payloadFromJson(null, ServerState.class));
    }

    @Test
    void oversizedServerStateIsCut() {
        List<ArenaInfo> arenas = new java.util.ArrayList<>();
        for (int i = 0; i < Limits.MAX_ARENAS_PER_SERVER + 30; i++) {
            ArenaInfo a = new ArenaInfo("a" + i, "g", ArenaInfo.State.WAITING, 0, 8, 1, 8, true);
            for (int j = 0; j < Limits.MAX_REJOIN_PER_ARENA + 10; j++) a.rejoin.add(new RejoinSlot(UUID.randomUUID(), 1));
            arenas.add(a);
        }
        arenas.add(new ArenaInfo("n".repeat(Limits.MAX_NAME_LENGTH + 1), null, ArenaInfo.State.WAITING, 0, 8, 1, 8, true));
        ServerState back = roundTrip(new ServerState("game1", "GAME", false, 0, arenas)).payload(ServerState.class);
        assertEquals(Limits.MAX_ARENAS_PER_SERVER, back.arenas.size());
        assertTrue(back.arenas.stream().allMatch(a -> a.rejoin.size() <= Limits.MAX_REJOIN_PER_ARENA));
        assertEquals(Limits.MAX_REJOIN_PER_SERVER, back.arenas.stream().mapToInt(a -> a.rejoin.size()).sum());
    }

    @Test
    void longNamesAreDropped() {
        String longName = "x".repeat(Limits.MAX_NAME_LENGTH + 1);
        ArenaInfo ok = new ArenaInfo("ok", longName, ArenaInfo.State.WAITING, 0, 8, 1, 8, true);
        ArenaInfo bad = new ArenaInfo(longName, null, ArenaInfo.State.WAITING, 0, 8, 1, 8, true);
        ServerState s = new ServerState("game1", "GAME", false, 0, List.of(ok, bad));
        ServerState back = roundTrip(s).payload(ServerState.class);
        assertEquals(1, back.arenas.size());
        assertNull(back.arenas.get(0).group);
        NetworkState n = roundTrip(new NetworkState(List.of(new ServerState(longName, "GAME", false, 0, List.of()), s)))
                .payload(NetworkState.class);
        assertEquals(1, n.servers.size());
        NetworkGroups g = roundTrip(new NetworkGroups(1, List.of(new NetworkGroups.Group(longName, null, List.of("a")),
                new NetworkGroups.Group("g", "PAPER", List.of("a", longName + ":" + longName + "x"))))).payload(NetworkGroups.class);
        assertEquals(1, g.groups.size());
        assertEquals(List.of("a"), g.groups.get(0).arenas);
    }

    @Test
    void networkStateServerCountAndGroupSizesAreCapped() {
        List<ServerState> servers = new java.util.ArrayList<>();
        for (int i = 0; i < Limits.MAX_SERVERS + 10; i++) servers.add(new ServerState("s" + i, "GAME", false, 0, List.of()));
        assertEquals(Limits.MAX_SERVERS, roundTrip(new NetworkState(servers)).payload(NetworkState.class).servers.size());
        List<String> entries = new java.util.ArrayList<>();
        for (int i = 0; i < Limits.MAX_GROUP_ENTRIES + 10; i++) entries.add("a" + i);
        NetworkGroups g = roundTrip(new NetworkGroups(1, List.of(new NetworkGroups.Group("g", null, entries)))).payload(NetworkGroups.class);
        assertEquals(Limits.MAX_GROUP_ENTRIES, g.groups.get(0).arenas.size());
    }

    @Test
    void ticketTtlIsAdditive() {
        JoinTicket t = roundTrip(new JoinTicket("r1", A, TargetKind.ANY, null, null, null, List.of(A), 99L, 1234L)).payload(JoinTicket.class);
        assertEquals(1234L, t.ttlMillis);
        JoinTicket old = decode("JOIN_TICKET", "{\"requestId\":\"r\",\"expiresAt\":5}", JoinTicket.class);
        assertEquals(0L, old.ttlMillis);
        assertEquals(5L, old.expiresAt);
    }

    @Test
    void stateFitDropsRejoinThenArenas() {
        List<ArenaInfo> arenas = new java.util.ArrayList<>();
        for (int i = 0; i < 40; i++) {
            ArenaInfo a = new ArenaInfo("arena" + i, "g", i < 20 ? ArenaInfo.State.PLAYING : ArenaInfo.State.WAITING, i, 16, 2, 8, true);
            for (int j = 0; j < 30; j++) a.rejoin.add(new RejoinSlot(UUID.randomUUID(), 1));
            arenas.add(a);
        }
        NetworkState small = new NetworkState(new java.util.ArrayList<>(List.of(new ServerState("s", "GAME", false, 0, new java.util.ArrayList<>(arenas)))));
        assertEquals(StateFit.Result.UNCHANGED, StateFit.fit(new NetworkState(), 100));
        assertEquals(StateFit.Result.REJOIN_DROPPED, StateFit.fit(small, 8_000));
        assertEquals(40, small.servers.get(0).arenas.size());
        NetworkState tight = new NetworkState(new java.util.ArrayList<>(List.of(new ServerState("s", "GAME", false, 0, new java.util.ArrayList<>(arenas)))));
        assertEquals(StateFit.Result.ARENAS_TRUNCATED, StateFit.fit(tight, 2_000));
        assertTrue(StateFit.size(tight) <= 2_000);
        assertTrue(tight.servers.get(0).arenas.stream().allMatch(ArenaInfo::isJoinable), "running arenas are cut first");
    }
}
