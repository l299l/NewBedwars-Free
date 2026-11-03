package com.l299l.newbedwars.proxy.core.routing;

import com.l299l.newbedwars.common.routing.ServerSelector;
import com.l299l.newbedwars.proxy.core.FakePlatform;
import com.l299l.newbedwars.proxy.core.party.PartyService;

import com.l299l.newbedwars.common.protocol.MessageType;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {
    FakePlatform pf;
    ServerRegistry servers;
    PartyService parties;
    Router router;
    UUID leo, bot;

    @BeforeEach
    void setUp() {
        pf = new FakePlatform();
        pf.knownServers.addAll(List.of("lobby", "lobby2", "game1", "game2"));
        servers = new ServerRegistry(List.of("game1", "game2"), List.of("lobby", "lobby2"), 15_000);
        parties = new PartyService(pf, 60_000);
        router = new Router(pf, servers, parties, 10_000);
        leo = pf.add("Leo", "lobby");
        bot = pf.add("Bot", "lobby");
    }

    private static ArenaInfo arena(String name, int players, int max, int perTeam) {
        return new ArenaInfo(name, "solo", ArenaInfo.State.WAITING, players, max, perTeam, max / perTeam, true);
    }

    private void state(String server, long now, ArenaInfo... arenas) {
        router.handle(server, null, NetMessage.of(server, new ServerState(server, "GAME", false, now, List.of(arenas))), now);
    }

    private void request(UUID p, TargetKind kind, String target, long now) {
        router.handle("lobby", p, NetMessage.of("lobby", new JoinRequest(p, kind, target)), now);
    }

    private List<JoinTicket> tickets() {
        return pf.toPlayerServer.stream().filter(m -> m.type() == MessageType.JOIN_TICKET)
                .map(m -> m.payload(JoinTicket.class)).toList();
    }

    @Test
    void joinGoesToFullestArenaWithTicketAfterConnect() {
        state("game1", 0, arena("a", 1, 8, 1));
        state("game2", 0, arena("b", 3, 8, 1));
        router.onServerConnected(leo, "lobby", 0);
        request(leo, TargetKind.GROUP, "solo", 100);
        assertEquals(List.of("Leo->game2"), pf.connects);
        router.onServerConnected(leo, "game2", 200);
        JoinTicket t = tickets().get(0);
        assertEquals(leo, t.player);
        assertEquals("b", t.arena);
        assertEquals(TargetKind.GROUP, t.kind);
        assertNull(t.partyId);
        assertEquals(4, servers.snapshot(200).servers.stream().filter(s -> s.serverId.equals("game2")).findFirst()
                .orElseThrow().arenas.get(0).players);
    }

    @Test
    void noServerGivesMessageAndPlayerStays() {
        servers = new ServerRegistry(List.of(), List.of("lobby"), 15_000);
        router = new Router(pf, servers, parties, 10_000);
        request(leo, TargetKind.ANY, null, 0);
        assertTrue(pf.has("Leo:NetworkNoServer"));
        assertTrue(pf.connects.isEmpty());
    }

    @Test
    void staleConfiguredServerIsTriedBlind() {
        request(leo, TargetKind.GROUP, "solo", 0);
        assertEquals(List.of("Leo->game1"), pf.connects);
        router.onServerConnected(leo, "game1", 10);
        JoinTicket t = tickets().get(0);
        assertNull(t.arena);
        assertEquals("solo", t.target);
    }

    @Test
    void rejectTriesNextServerOnceThenLobby() {
        state("game1", 0, arena("a", 3, 8, 1));
        state("game2", 0, arena("b", 1, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        router.onServerConnected(leo, "game1", 1);
        String id = tickets().get(0).requestId;
        router.handle("game1", leo, NetMessage.of("game1", new JoinReject(id, leo, "a", JoinReject.Reason.FULL)), 2);
        // a duplicate reject (e.g. from a party member) is ignored
        router.handle("game1", leo, NetMessage.of("game1", new JoinReject(id, leo, "a", JoinReject.Reason.FULL)), 2);
        assertEquals(List.of("Leo->game1", "Leo->game2"), pf.connects);
        router.onServerConnected(leo, "game2", 3);
        assertEquals("b", tickets().get(1).arena);
        router.handle("game2", leo, NetMessage.of("game2", new JoinReject(id, leo, "b", JoinReject.Reason.FULL)), 4);
        assertTrue(pf.has("Leo:NetworkNoServerAfterTry"));
        assertEquals("Leo->lobby2", pf.connects.get(2), "least full lobby (lobby has Bot)");
    }

    @Test
    void rejectFromOtherServerIsIgnored() {
        state("game1", 0, arena("a", 3, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        String id = "r1-0";
        router.handle("game2", leo, NetMessage.of("game2", new JoinReject(id, leo, "a", JoinReject.Reason.FULL)), 2);
        assertEquals(1, pf.connects.size());
    }

    @Test
    void failedConnectReroutes() {
        state("game1", 0, arena("a", 3, 8, 1));
        state("game2", 0, arena("b", 1, 8, 1));
        pf.failing.add("game1");
        request(leo, TargetKind.ANY, null, 0);
        assertEquals(List.of("Leo->game1", "Leo->game2"), pf.connects);
    }

    @Test
    void partyAdminBringsPartyNonAdminIsRefused() {
        parties.invite(leo, "Bot", 0);
        parties.accept(bot);
        state("game1", 0, arena("duo", 0, 8, 2));
        request(bot, TargetKind.ANY, null, 0);
        assertTrue(pf.has("Bot:PartyOnlyAdminCanJoin{/player/=Leo}"));
        assertTrue(pf.connects.isEmpty());
        request(leo, TargetKind.ANY, null, 1);
        assertEquals(List.of("Leo->game1", "Bot->game1"), pf.connects);
        router.onServerConnected(bot, "game1", 2);
        JoinTicket t = tickets().get(0);
        assertEquals(List.of(leo, bot), t.party);
        assertEquals(parties.partyOf(leo).id(), t.partyId);
    }

    @Test
    void partyDoesNotFitSoloArena() {
        parties.invite(leo, "Bot", 0);
        parties.accept(bot);
        state("game1", 0, arena("solo", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        // only the blind try of game2 is left (no fresh state)
        assertEquals(List.of("Leo->game2", "Bot->game2"), pf.connects);
    }

    @Test
    void helloResendsTicketAndLobbyGetsState() {
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        router.handle("game1", leo, NetMessage.of("game1", new Hello("game1", "GAME", "x")), 1);
        assertEquals(1, tickets().size());
        pf.toPlayerServer.clear();
        router.handle("lobby", bot, NetMessage.of("lobby", new Hello("lobby", "LOBBY", "x")), 1);
        assertEquals(MessageType.NETWORK_STATE, pf.toPlayerServer.get(0).type());
    }

    @Test
    void sendToLobbyPicksLeastFullOther() {
        UUID g = pf.add("Gamer", "game1");
        router.handle("game1", g, NetMessage.of("game1", new SendToLobby(List.of(g))), 0);
        assertEquals(List.of("Gamer->lobby2"), pf.connects);
    }

    @Test
    void pushStateOnlyWhenChangedUnlessForced() {
        router.pushState(0, false);
        assertTrue(pf.toServer.isEmpty());
        state("game1", 0, arena("a", 0, 8, 1));
        router.pushState(0, false);
        // lobbies list the arenas; game servers need them for the map vote across servers
        assertEquals(List.of("lobby:NETWORK_STATE", "lobby2:NETWORK_STATE", "game1:NETWORK_STATE", "game2:NETWORK_STATE"), pf.toServer);
        router.pushState(0, false);
        assertEquals(4, pf.toServer.size());
        router.pushState(0, true);
        assertEquals(8, pf.toServer.size());
    }

    @Test
    void lobbyStatesAreIgnoredAndStaleServersHidden() {
        state("lobby", 0, arena("x", 0, 8, 1));
        state("game1", 0, arena("a", 0, 8, 1));
        assertEquals(1, servers.snapshot(0).servers.size());
        assertEquals(0, servers.snapshot(15_001).servers.size());
    }

    @Test
    void tickDropsOldRequests() {
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        assertEquals(1, router.pendingCount());
        router.tick(20_001);
        assertEquals(0, router.pendingCount());
    }

    private void runningWithSlot(String server, UUID player, long expires) {
        ArenaInfo running = new ArenaInfo("r", "solo", ArenaInfo.State.PLAYING, 3, 8, 1, 8, true);
        running.rejoin.add(new RejoinSlot(player, expires));
        router.handle(server, null, NetMessage.of(server, new ServerState(server, "GAME", false, 0, List.of(running))), 0);
    }

    @Test
    void rejoinGoesToTheSlotsServerWithARejoinTicket() {
        runningWithSlot("game2", leo, 60_000);
        request(leo, TargetKind.REJOIN, null, 10);
        assertEquals(List.of("Leo->game2"), pf.connects);
        router.onServerConnected(leo, "game2", 20);
        JoinTicket t = tickets().get(0);
        assertEquals(TargetKind.REJOIN, t.kind);
        assertEquals("r", t.arena);
    }

    @Test
    void rejoinWithoutSlotOrExpiredGivesMessage() {
        request(leo, TargetKind.REJOIN, null, 0);
        assertTrue(pf.has("Leo:NetworkNoRejoin"));
        runningWithSlot("game1", leo, 5_000);
        request(leo, TargetKind.REJOIN, null, 6_000);
        assertEquals(2, pf.messages.stream().filter(m -> m.equals("Leo:NetworkNoRejoin")).count());
        assertTrue(pf.connects.isEmpty());
    }

    @Test
    void rejoinIsNotRetriedElsewhere() {
        runningWithSlot("game1", leo, 60_000);
        state("game2", 0, arena("b", 0, 8, 1));
        request(leo, TargetKind.REJOIN, null, 10);
        router.onServerConnected(leo, "game1", 11);
        String id = tickets().get(0).requestId;
        router.handle("game1", leo, NetMessage.of("game1", new JoinReject(id, leo, "r", JoinReject.Reason.OTHER)), 12);
        assertTrue(pf.has("Leo:NetworkRejoinFailed"));
        assertEquals(List.of("Leo->game1", "Leo->lobby2"), pf.connects);
    }

    @Test
    void partyMemberMayRejoinAlone() {
        parties.invite(leo, "Bot", 0);
        parties.accept(bot);
        runningWithSlot("game1", bot, 60_000);
        request(bot, TargetKind.REJOIN, null, 10);
        assertEquals(List.of("Bot->game1"), pf.connects);
    }

    private void running(String server, String name) {
        ArenaInfo a = new ArenaInfo(name, "solo", ArenaInfo.State.PLAYING, 4, 8, 1, 8, true);
        router.handle(server, null, NetMessage.of(server, new ServerState(server, "GAME", false, 0, List.of(a))), 0);
    }

    @Test
    void spectateGoesToTheRunningGame() {
        running("game2", "TestArena");
        request(leo, TargetKind.SPECTATE, "testarena", 5);
        assertEquals(List.of("Leo->game2"), pf.connects);
        router.onServerConnected(leo, "game2", 6);
        JoinTicket t = tickets().get(0);
        assertEquals(TargetKind.SPECTATE, t.kind);
        assertEquals("TestArena", t.arena);
    }

    @Test
    void spectateWithoutRunningGameOrOnOtherServer() {
        request(leo, TargetKind.SPECTATE, "Nope", 0);
        assertTrue(pf.has("Leo:NetworkNoSpectate{/arena/=Nope}"));
        running("game1", "TestArena");
        router.handle("lobby", leo, NetMessage.of("lobby", new JoinRequest(leo, TargetKind.SPECTATE, "TestArena", "game2", null)), 1);
        assertTrue(pf.connects.isEmpty(), "only game2 was asked for");
    }

    @Test
    void spectateRejectIsNotRetried() {
        running("game1", "TestArena");
        running("game2", "TestArena");
        request(leo, TargetKind.SPECTATE, "TestArena", 1);
        router.onServerConnected(leo, pf.servers.get(leo), 2);
        JoinTicket t = tickets().get(0);
        String srv = pf.servers.get(leo);
        router.handle(srv, leo, NetMessage.of(srv, new JoinReject(t.requestId, leo, "TestArena", JoinReject.Reason.NOT_JOINABLE)), 3);
        assertTrue(pf.has("Leo:NetworkSpectateFailed"));
        assertEquals(2, pf.connects.size(), "game server, then back to a lobby");
    }

    @Test
    void voteMoveTakesExactlyThePlayersToThatServer() {
        UUID g1 = pf.add("G1", "game1");
        UUID g2 = pf.add("G2", "game1");
        state("game1", 0, arena("TestArena", 0, 8, 2));
        state("game2", 0, arena("TestArena", 3, 8, 2));
        // not admins, no party: the game server chose them, so no party rule
        router.handle("game1", g1, NetMessage.of("game1", new JoinRequest(g1, TargetKind.ARENA, "TestArena", "game2", List.of(g1, g2))), 1);
        assertEquals(List.of("G1->game2", "G2->game2"), pf.connects);
        router.onServerConnected(g1, "game2", 2);
        JoinTicket t = tickets().get(0);
        assertEquals("TestArena", t.arena);
        assertEquals(List.of(g1, g2), t.party);
        assertNotNull(t.partyId, "moved together");
    }

    @Test
    void voteMoveFailsBackToLobbyWhenThatServerHasNoRoom() {
        UUID g1 = pf.add("G1", "game1");
        state("game2", 0, new ArenaInfo("TestArena", "solo", ArenaInfo.State.PLAYING, 3, 8, 1, 8, true));
        router.handle("game1", g1, NetMessage.of("game1", new JoinRequest(g1, TargetKind.ARENA, "TestArena", "game2", List.of(g1))), 1);
        assertTrue(pf.has("G1:NetworkNoServer"));
        assertEquals(List.of("G1->lobby2"), pf.connects);
    }

    private void groups(String from, long revision) {
        router.handle(from, leo, NetMessage.of(from, new com.l299l.newbedwars.common.protocol.payload.NetworkGroups(revision,
                List.of(new com.l299l.newbedwars.common.protocol.payload.NetworkGroups.Group("solo", "PAPER", List.of("a"))))), 0);
    }

    @Test
    void groupsFromLobbyGoToEveryOtherServerNewestWins() {
        groups("lobby", 5);
        assertEquals(List.of("lobby2:GROUPS", "game1:GROUPS", "game2:GROUPS"), pf.toServer);
        pf.toServer.clear();
        groups("lobby2", 4);   // older: ignored
        groups("game1", 9);    // game servers do not define groups
        assertTrue(pf.toServer.isEmpty());
        assertEquals(5, router.groups().current().revision);
        groups("lobby2", 6);
        assertEquals(List.of("lobby:GROUPS", "game1:GROUPS", "game2:GROUPS"), pf.toServer);
    }

    @Test
    void lobbyGroupsLabelArenasOfEveryServer() {
        state("game1", 0, arena("a", 0, 8, 1), arena("b", 0, 8, 1));
        state("game2", 0, arena("a", 0, 8, 1));
        router.handle("lobby", leo, NetMessage.of("lobby", new com.l299l.newbedwars.common.protocol.payload.NetworkGroups(1,
                List.of(new com.l299l.newbedwars.common.protocol.payload.NetworkGroups.Group("duels", "PAPER", List.of("a", "game1:b"))))), 0);
        var snap = servers.snapshot(0).servers;
        assertEquals("duels", snap.get(0).arenas.get(0).group);
        assertEquals("duels", snap.get(0).arenas.get(1).group);
        assertEquals("duels", snap.get(1).arenas.get(0).group);
        // a game server still reporting its old group is relabelled
        state("game2", 1, arena("a", 0, 8, 1));
        assertEquals("duels", servers.snapshot(1).servers.get(1).arenas.get(0).group);
        request(leo, TargetKind.GROUP, "duels", 2);
        assertEquals(1, pf.connects.size());
        request(bot, TargetKind.GROUP, "solo", 2);
        assertTrue(pf.messages.stream().anyMatch(m -> m.startsWith("Bot:NetworkNoServer")));
    }

    @Test
    void helloGetsNewestGroups() {
        UUID g = pf.add("Gamer", "game1");
        router.handle("game1", g, NetMessage.of("game1", new Hello("game1", "GAME", "x")), 1);
        assertTrue(pf.toPlayerServer.stream().noneMatch(m -> m.type() == MessageType.GROUPS));
        groups("lobby", 5);
        router.handle("game1", g, NetMessage.of("game1", new Hello("game1", "GAME", "x")), 2);
        assertEquals(1, pf.toPlayerServer.stream().filter(m -> m.type() == MessageType.GROUPS).count());
    }

    @Test
    void arenaJoinPinnedToOneServer() {
        state("game1", 0, arena("castle", 3, 8, 1));
        state("game2", 0, arena("castle", 1, 8, 1));
        router.handle("lobby", leo, NetMessage.of("lobby", new JoinRequest(leo, TargetKind.ARENA, "castle", "game2", null)), 0);
        assertEquals(List.of("Leo->game2"), pf.connects);
    }


    @Test
    void joinRequestMustComeFromTheRequestedPlayersOwnConnection() {
        state("game1", 0, arena("a", 0, 8, 1));
        router.handle("lobby", bot, NetMessage.of("lobby", new JoinRequest(leo, TargetKind.ANY, null)), 0);
        router.handle("lobby", null, NetMessage.of("lobby", new JoinRequest(leo, TargetKind.ANY, null)), 0);
        router.handle("lobby", bot, NetMessage.of("lobby", new JoinRequest(leo, TargetKind.REJOIN, null)), 0);
        router.handle("lobby", bot, NetMessage.of("lobby", new JoinRequest(leo, TargetKind.SPECTATE, "a")), 0);
        assertTrue(pf.connects.isEmpty());
        assertTrue(pf.messages.isEmpty());
        assertEquals(0, router.pendingCount());
    }

    @Test
    void malformedJoinRequestsAreIgnored() {
        router.onJoinRequest("lobby", leo, null, 0);
        router.onJoinRequest("lobby", leo, new JoinRequest(), 0);
        JoinRequest noKind = new JoinRequest(leo, null, null);
        noKind.players = null;
        state("game1", 0, arena("a", 0, 8, 1));
        router.onJoinRequest("lobby", leo, noKind, 0);
        assertEquals(List.of("Leo->game1"), pf.connects, "no kind means any");
    }

    @Test
    void tooLongTargetIsIgnored() {
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.GROUP, "*".repeat(ServerSelector.MAX_TARGET_LENGTH + 1), 0);
        assertTrue(pf.connects.isEmpty());
        request(leo, TargetKind.GROUP, "*".repeat(ServerSelector.MAX_TARGET_LENGTH), 0);
        assertEquals(List.of("Leo->game1"), pf.connects);
    }

    @Test
    void voteListFromALobbyOrWithForeignPlayersIsIgnored() {
        UUID g1 = pf.add("G1", "game1");
        UUID other = pf.add("Other", "game2");
        state("game2", 0, arena("TestArena", 0, 8, 1));
        router.handle("lobby", leo, NetMessage.of("lobby",
                new JoinRequest(leo, TargetKind.ARENA, "TestArena", "game2", List.of(leo, bot))), 1);
        assertTrue(pf.connects.isEmpty(), "lobbies never choose players");
        router.handle("game1", g1, NetMessage.of("game1",
                new JoinRequest(g1, TargetKind.ARENA, "TestArena", "game2", List.of(other))), 1);
        assertTrue(pf.connects.isEmpty(), "other is not on game1");
        router.handle("game1", g1, NetMessage.of("game1",
                new JoinRequest(g1, TargetKind.ARENA, "TestArena", "game2", List.of(g1, other))), 1);
        assertEquals(List.of("G1->game2"), pf.connects);
    }

    @Test
    void malformedRejectsAreIgnored() {
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        router.onReject("game1", null, 1);
        router.onReject("game1", new JoinReject(), 1);
        router.onReject("game1", new JoinReject("nope", leo, "a", JoinReject.Reason.FULL), 1);
        assertEquals(1, pf.connects.size());
        assertEquals(1, router.pendingCount());
    }

    @Test
    void helloWithoutPlayerConnectionDoesNothing() {
        router.handle("game1", null, NetMessage.of("game1", new Hello()), 0);
        router.handle("game1", leo, NetMessage.of("game1", new Hello()), 0);
        assertEquals(1, pf.toPlayerServer.size(), "only the connected player is answered");
        assertEquals(leo, pf.toPlayerServerVia.get(0));
    }

    @Test
    void serverStateIsStoredUnderTheSendersIdNotThePayloads() {
        router.handle("game1", null, NetMessage.of("game1",
                new ServerState("game2", "GAME", false, 0, List.of(arena("a", 0, 8, 1)))), 0);
        var snap = servers.snapshot(0).servers;
        assertEquals(1, snap.size());
        assertEquals("game1", snap.get(0).serverId);
        assertFalse(servers.isFresh("game2", 0));
    }

    @Test
    void serverStateFromALobbyIsIgnored() {
        router.handle("lobby", leo, NetMessage.of("lobby",
                new ServerState("lobby", "GAME", false, 0, List.of(arena("a", 0, 8, 1)))), 0);
        assertTrue(servers.snapshot(0).servers.isEmpty());
        assertFalse(servers.isGame("lobby"));
    }

    @Test
    void serverStateWithNullArenasOrNamesDoesNotBreakRouting() {
        ServerState bad = new ServerState("game1", "GAME", false, 0, null);
        router.handle("game1", null, NetMessage.of("game1", bad), 0);
        ServerState noName = new ServerState("game2", "GAME", false, 0, java.util.Arrays.asList(null, arena(null, 0, 8, 1), arena("ok", 0, 8, 1)));
        router.handle("game2", null, NetMessage.of("game2", noName), 0);
        request(leo, TargetKind.GROUP, "o*", 1);
        assertEquals(List.of("Leo->game2"), pf.connects);
    }

    @Test
    void groupsFromAGameServerAreIgnored() {
        groups("game1", 9);
        assertTrue(pf.toServer.isEmpty());
        assertNull(router.groups().current());
    }

    @Test
    void sendToLobbyIgnoresNullPlayersAndPlayersOnOtherServers() {
        UUID g = pf.add("Gamer", "game1");
        UUID h = pf.add("Hiker", "game2");
        router.handle("game1", g, NetMessage.of("game1", new SendToLobby(null)), 0);
        router.handle("game1", g, NetMessage.of("game1", new SendToLobby(java.util.Arrays.asList(null, h, bot))), 0);
        assertTrue(pf.connects.isEmpty());
        router.handle("game1", g, NetMessage.of("game1", new SendToLobby(java.util.Arrays.asList(null, g))), 0);
        assertEquals(List.of("Gamer->lobby2"), pf.connects);
    }


    private int playersIn(String server, long now) {
        return servers.snapshot(now).servers.stream().filter(x -> x.serverId.equals(server)).findFirst()
                .orElseThrow().arenas.get(0).players;
    }

    private void longLivedRegistry() {
        servers = new ServerRegistry(List.of("game1", "game2"), List.of("lobby", "lobby2"), 1_000_000);
        router = new Router(pf, servers, parties, 10_000);
    }

    @Test
    void rejectedJoinReleasesItsReservationBeforeTryingElsewhere() {
        state("game1", 0, arena("a", 3, 8, 1));
        state("game2", 0, arena("b", 1, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        assertEquals(4, playersIn("game1", 0));
        router.handle("game1", leo, NetMessage.of("game1", new JoinReject("r1-0", leo, "a", JoinReject.Reason.FULL)), 1);
        assertEquals(3, playersIn("game1", 1));
        assertEquals(2, playersIn("game2", 1));
    }

    @Test
    void failedAndExpiredJoinsReleaseTheirReservation() {
        longLivedRegistry();
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        assertEquals(1, playersIn("game1", 0));
        router.tick(20_001);
        assertEquals(0, router.pendingCount());
        assertEquals(0, playersIn("game1", 20_001));

        pf.servers.put(leo, "lobby");
        pf.failing.addAll(List.of("game1", "game2"));
        request(leo, TargetKind.ANY, null, 30_000);
        assertEquals(0, playersIn("game1", 30_000), "no server could be reached: nothing stays reserved");
    }

    @Test
    void aFreshStateReplacesTheReservationAndLaterReleaseDoesNotDoubleCount() {
        longLivedRegistry();
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        state("game1", 100, arena("a", 1, 8, 1));
        assertEquals(1, playersIn("game1", 100));
        router.tick(20_001);
        assertEquals(1, playersIn("game1", 20_001), "the real player must stay counted");
    }

    @Test
    void newJoinReplacesOldPendingAndItsReservation() {
        longLivedRegistry();
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        request(leo, TargetKind.ANY, null, Router.REQUEST_COOLDOWN_MILLIS);
        assertEquals(1, router.pendingCount());
        assertEquals(1, playersIn("game1", Router.REQUEST_COOLDOWN_MILLIS));
    }

    @Test
    void disconnectedPlayerReleasesTheirReservation() {
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        router.onDisconnect(leo);
        assertEquals(0, router.pendingCount());
        assertEquals(0, playersIn("game1", 0));
    }

    @Test
    void repeatedJoinRequestsWithinTheCooldownAreIgnored() {
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 0);
        request(leo, TargetKind.ANY, null, 100);
        request(leo, TargetKind.GROUP, "solo", Router.REQUEST_COOLDOWN_MILLIS - 1);
        assertEquals(List.of("Leo->game1"), pf.connects);
        assertEquals(1, playersIn("game1", 0));
        pf.servers.put(leo, "lobby");
        request(leo, TargetKind.ANY, null, Router.REQUEST_COOLDOWN_MILLIS);
        assertEquals(2, pf.connects.size());
    }

    @Test
    void rejoinAndSpectateAreNotHeldBackByTheCooldown() {
        state("game1", 0, arena("a", 0, 8, 1));
        runningWithSlot("game2", leo, 60_000);
        request(leo, TargetKind.ANY, null, 0);
        request(leo, TargetKind.REJOIN, null, 10);
        assertEquals(List.of("Leo->game1", "Leo->game2"), pf.connects);
        assertEquals(0, playersIn("game1", 10), "the replaced join gave its place back");
    }

    @Test
    void ticketCarriesRelativeTtl() {
        state("game1", 0, arena("a", 0, 8, 1));
        request(leo, TargetKind.ANY, null, 1_000);
        router.onServerConnected(leo, "game1", 3_000);
        JoinTicket t = tickets().get(0);
        assertEquals(11_000, t.expiresAt);
        assertEquals(8_000, t.ttlMillis);
    }

    @Test
    void groupsWithARevisionFarInTheFutureAreIgnored() {
        router.handle("lobby", leo, NetMessage.of("lobby", new NetworkGroups(Long.MAX_VALUE, List.of())), 1_000);
        assertNull(router.groups().current());
        assertTrue(pf.toServer.isEmpty());
        router.handle("lobby", leo, NetMessage.of("lobby",
                new NetworkGroups(1_000 + 2L * 24 * 60 * 60 * 1000, List.of())), 1_000);
        assertNull(router.groups().current());
        router.handle("lobby", leo, NetMessage.of("lobby", new NetworkGroups(1_000 + 60_000, List.of())), 1_000);
        assertNotNull(router.groups().current());
        router.handle("lobby", leo, NetMessage.of("lobby", new NetworkGroups(Long.MAX_VALUE, List.of())), 1_000);
        assertEquals(1_000 + 60_000, router.groups().current().revision);
    }

    @Test
    void stateFromAnUnconfiguredServerIsIgnored() {
        state("evil", 0, arena("a", 0, 8, 1));
        assertFalse(servers.isGame("evil"));
        assertTrue(servers.snapshot(0).servers.isEmpty());
        assertEquals(0, pf.toServer.size());
    }
}
