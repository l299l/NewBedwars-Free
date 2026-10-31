package com.l299l.newbedwars.proxy.core.routing;

import com.l299l.newbedwars.proxy.core.ProxyPlatform;
import com.l299l.newbedwars.proxy.core.group.GroupRelay;
import com.l299l.newbedwars.proxy.core.party.PartyService;
import com.l299l.newbedwars.proxy.core.party.ProxyParty;

import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.routing.ServerSelector;
import com.l299l.newbedwars.common.protocol.payload.Hello;
import com.l299l.newbedwars.common.protocol.payload.JoinReject;
import com.l299l.newbedwars.common.protocol.payload.JoinRequest;
import com.l299l.newbedwars.common.protocol.payload.JoinTicket;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles messages from backends: server states, join requests (pick a server with {@link ServerSelector},
 * move the player and their party, hand the game server a {@link JoinTicket} once they arrived), rejects
 * (one more try elsewhere, then back to a lobby) and send-to-lobby. Not thread-safe.
 */
public final class Router {
    /** How many servers one request may try before the players go back to the lobby. */
    static final int MAX_TRIES = 2;

    private final ProxyPlatform platform;
    private final ServerRegistry servers;
    private final PartyService parties;
    private final GroupRelay groups;
    private final long ticketMillis;
    private final Map<String, Pending> byRequest = new HashMap<>();
    private java.util.function.Consumer<String> log = text -> {};
    private final Map<UUID, Pending> byPlayer = new HashMap<>();
    private int nextId;

    static final class Pending {
        final String id;
        final List<UUID> players;
        final String partyId;
        final TargetKind kind;
        final String target;
        final Set<String> tried = new HashSet<>();
        final Set<UUID> ticketsSent = new HashSet<>();
        /** Only this game server (map vote winner); null = any. */
        String onlyServer;
        ServerSelector.Choice current;
        long expiresAt;

        Pending(String id, List<UUID> players, String partyId, TargetKind kind, String target) {
            this.id = id;
            this.players = players;
            this.partyId = partyId;
            this.kind = kind;
            this.target = target;
        }
    }

    public Router(ProxyPlatform platform, ServerRegistry servers, PartyService parties, long ticketMillis) {
        this.platform = platform;
        this.servers = servers;
        this.parties = parties;
        this.groups = new GroupRelay(platform);
        this.ticketMillis = ticketMillis;
    }

    public void setLogger(java.util.function.Consumer<String> log) {
        this.log = log;
    }

    public ServerRegistry servers() {
        return servers;
    }

    public GroupRelay groups() {
        return groups;
    }

    /** A message from backend {@code server}, received through {@code via}. */
    public void handle(String server, UUID via, NetMessage msg, long now) {
        switch (msg.type()) {
            case HELLO -> onHello(server, via, msg.payload(Hello.class));
            case SERVER_STATE -> servers.update(server, msg.payload(ServerState.class), now);
            case JOIN_REQUEST -> onJoinRequest(server, msg.payload(JoinRequest.class), now);
            case JOIN_REJECT -> onReject(server, msg.payload(JoinReject.class), now);
            case GROUPS -> {
                // Only lobby servers define groups.
                if (servers.isLobby(server)
                        && groups.onGroups(server, msg.payload(NetworkGroups.class), servers.lobbyServers(), servers.gameServers())) {
                    servers.setGroups(groups.current());
                }
            }
            case SEND_TO_LOBBY -> {
                for (UUID p : msg.payload(com.l299l.newbedwars.common.protocol.payload.SendToLobby.class).players) {
                    sendToLobby(p);
                }
            }
            default -> {
                // NETWORK_STATE, JOIN_TICKET and PARTY_SYNC only go from the proxy to backends.
            }
        }
    }

    // ----- joining -----

    void onJoinRequest(String server, JoinRequest r, long now) {
        if (r == null || r.player == null) return;
        UUID requester = r.player;
        if (r.kind == TargetKind.REJOIN) {
            onRejoinRequest(requester, now);
            return;
        }
        if (r.kind == TargetKind.SPECTATE) {
            onSpectateRequest(requester, r.target, r.server, now);
            return;
        }
        List<UUID> group = new ArrayList<>();
        String partyId = null;
        if (r.players != null && !r.players.isEmpty()) {
            // A game server chose who moves together (map vote): no party-admin rule.
            for (UUID u : r.players) if (u != null && platform.isOnline(u) && !group.contains(u)) group.add(u);
            if (group.isEmpty()) return;
        } else {
            ProxyParty party = parties.partyOf(requester);
            if (party != null) {
                if (!party.isAdmin(requester)) {
                    String admin = platform.nameOf(party.admin());
                    platform.message(requester, "PartyOnlyAdminCanJoin", Map.of("/player/", admin == null ? "?" : admin));
                    return;
                }
                group.add(requester);
                for (UUID m : party.members()) {
                    // Members that are in a game on another server stay there.
                    if (!m.equals(requester) && platform.isOnline(m) && !servers.isGame(platform.currentServer(m))) group.add(m);
                }
                partyId = party.id();
            } else {
                group.add(requester);
            }
        }
        cancel(group);
        String id = "r" + (++nextId) + "-" + Long.toHexString(now);
        if (partyId == null && group.size() > 1) partyId = id;
        Pending p = new Pending(id, group, partyId, r.kind == null ? TargetKind.ANY : r.kind, r.target);
        p.onlyServer = r.server;
        byRequest.put(p.id, p);
        for (UUID u : group) byPlayer.put(u, p);
        route(p, now);
    }

    /** Watch a running game: to the server where {@code arena} is playing (or {@code onlyServer}), no retry. */
    private void onSpectateRequest(UUID player, String arena, String onlyServer, long now) {
        List<ServerSelector.Candidate> candidates = servers.candidates(now);
        if (onlyServer != null) candidates.removeIf(c -> !c.serverId.equalsIgnoreCase(onlyServer));
        ServerSelector.Choice where = ServerSelector.findRunning(candidates, arena);
        if (where == null) {
            platform.message(player, "NetworkNoSpectate", Map.of("/arena/", arena == null ? "?" : arena));
            return;
        }
        routeDirect(player, where, TargetKind.SPECTATE, now);
    }

    /** Sends the player back to the game they left; parties do not follow (each member rejoins alone). */
    private void onRejoinRequest(UUID player, long now) {
        ServerSelector.Choice where = ServerSelector.findRejoin(servers.candidates(now), player, now);
        if (where == null) {
            platform.message(player, "NetworkNoRejoin", Map.of());
            return;
        }
        routeDirect(player, where, TargetKind.REJOIN, now);
    }

    /** One player to one known server and arena (rejoin, spectate): no other server is tried. */
    private void routeDirect(UUID player, ServerSelector.Choice where, TargetKind kind, long now) {
        cancel(List.of(player));
        Pending p = new Pending("r" + (++nextId) + "-" + Long.toHexString(now), new ArrayList<>(List.of(player)), null,
                kind, where.arena);
        byRequest.put(p.id, p);
        byPlayer.put(player, p);
        p.current = where;
        p.tried.add(where.serverId);
        p.expiresAt = now + ticketMillis;
        log.accept(kind + " " + p.id + " -> " + platform.nameOf(player) + " to " + where);
        if (where.serverId.equals(platform.currentServer(player))) {
            sendTicket(p, player);
            return;
        }
        String server = where.serverId;
        platform.connect(player, server, ok -> {
            if (!ok) onConnectFailed(p, server);
        });
    }

    private void route(Pending p, long now) {
        List<ServerSelector.Candidate> candidates = servers.candidates(now);
        if (p.onlyServer != null) candidates.removeIf(c -> !c.serverId.equalsIgnoreCase(p.onlyServer));
        List<ServerSelector.Choice> choices = ServerSelector.choose(candidates, p.kind, p.target,
                p.players.size(), p.tried);
        if (choices.isEmpty() || p.tried.size() >= MAX_TRIES) {
            fail(p, p.tried.isEmpty() ? "NetworkNoServer" : "NetworkNoServerAfterTry");
            return;
        }
        ServerSelector.Choice choice = choices.get(0);
        log.accept("Join " + p.id + " (" + p.kind + (p.target == null ? "" : " " + p.target) + ", " + p.players.size()
                + " player(s)) -> " + choice + (choice.isBlind() ? " (no fresh state, the server decides)" : ""));
        p.current = choice;
        p.tried.add(choice.serverId);
        p.expiresAt = now + ticketMillis;
        if (!choice.isBlind()) servers.reserve(choice.serverId, choice.arena, p.players.size());
        for (UUID u : p.players) {
            if (choice.serverId.equals(platform.currentServer(u))) {
                // Already there (e.g. a retry on the same server): the ticket can go right away.
                sendTicket(p, u);
                continue;
            }
            String server = choice.serverId;
            platform.connect(u, server, ok -> {
                if (!ok) onConnectFailed(p, server);
            });
        }
    }

    /** The player arrived on {@code server} (connection switch done): give them their ticket. */
    public void onServerConnected(UUID player, String server, long now) {
        parties.onServerSwitch(player);
        Pending p = byPlayer.get(player);
        if (p != null && p.current != null && server.equals(p.current.serverId)) sendTicket(p, player);
        if (servers.isLobby(server) || servers.isGame(server)) {
            platform.sendToPlayerServer(player, NetMessage.of(PartyService.FROM, servers.snapshot(now)));
        }
    }

    private void onHello(String server, UUID via, Hello h) {
        if (via == null) return;
        // The backend is ready for messages now: resend what it may have missed while the player was connecting.
        parties.onServerSwitch(via);
        Pending p = byPlayer.get(via);
        if (p != null && p.current != null && server.equals(p.current.serverId)) sendTicket(p, via);
        if (servers.isLobby(server) || servers.isGame(server)) {
            platform.sendToPlayerServer(via, NetMessage.of(PartyService.FROM, servers.snapshot(System.currentTimeMillis())));
            groups.onHello(via);
        }
    }

    private void sendTicket(Pending p, UUID player) {
        if (p.ticketsSent.add(player)) log.accept("Ticket " + p.id + " -> " + platform.nameOf(player) + " on " + p.current.serverId);
        JoinTicket t = new JoinTicket(p.id, player, p.kind, p.target, p.current.arena, p.partyId,
                new ArrayList<>(p.players), p.expiresAt);
        platform.sendToPlayerServer(player, NetMessage.of(PartyService.FROM, t));
    }

    void onReject(String server, JoinReject r, long now) {
        if (r == null) return;
        Pending p = byRequest.get(r.requestId);
        // Only the first reject of the current try counts (a party sends one per member).
        if (p == null || p.current == null || !server.equals(p.current.serverId)) return;
        log.accept("Join " + p.id + " rejected by " + server + ": " + r.reason);
        p.current = null;
        if (p.kind == TargetKind.REJOIN || p.kind == TargetKind.SPECTATE) {
            fail(p, p.kind == TargetKind.REJOIN ? "NetworkRejoinFailed" : "NetworkSpectateFailed");
            return;
        }
        route(p, now);
    }

    private void onConnectFailed(Pending p, String server) {
        if (p.current == null || !server.equals(p.current.serverId)) return;
        p.current = null;
        if (p.kind == TargetKind.REJOIN || p.kind == TargetKind.SPECTATE) {
            fail(p, p.kind == TargetKind.REJOIN ? "NetworkRejoinFailed" : "NetworkSpectateFailed");
            return;
        }
        route(p, System.currentTimeMillis());
    }

    private void fail(Pending p, String key) {
        log.accept("Join " + p.id + " found no server (tried " + p.tried + ")");
        remove(p);
        for (UUID u : p.players) {
            platform.message(u, key, Map.of());
            if (!servers.isLobby(platform.currentServer(u))) sendToLobby(u);
        }
    }

    /** A ticket was used (the player is in the arena) or the request is over. */
    private void remove(Pending p) {
        byRequest.remove(p.id);
        for (UUID u : p.players) byPlayer.remove(u, p);
    }

    private void cancel(List<UUID> players) {
        for (UUID u : players) {
            Pending old = byPlayer.get(u);
            if (old != null) remove(old);
        }
    }

    public void onDisconnect(UUID player) {
        Pending p = byPlayer.remove(player);
        if (p != null) p.players.remove(player);
    }

    /** Drops requests whose ticket time is over (they joined, or the game server sent them back). */
    public void tick(long now) {
        for (Iterator<Pending> it = byRequest.values().iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.expiresAt > 0 && p.expiresAt + ticketMillis < now) {
                it.remove();
                for (UUID u : p.players) byPlayer.remove(u, p);
            }
        }
    }

    // ----- lobby -----

    /** Moves a player to the least full lobby server (not the one they are on). */
    public void sendToLobby(UUID player) {
        String current = platform.currentServer(player);
        String best = null;
        int bestCount = Integer.MAX_VALUE;
        for (String lobby : servers.lobbyServers()) {
            if (lobby.equals(current) || !platform.serverExists(lobby)) continue;
            int count = platform.playerCount(lobby);
            if (count < bestCount) {
                best = lobby;
                bestCount = count;
            }
        }
        if (best != null) platform.connect(player, best, ok -> {});
    }

    /** Pushes the network state to every lobby server when it changed. */
    public void pushState(long now, boolean force) {
        if (!servers.takeDirty() && !force) return;
        NetMessage msg = NetMessage.of(PartyService.FROM, servers.snapshot(now));
        for (String lobby : servers.lobbyServers()) platform.sendToServer(lobby, msg);
        // Game servers need it too: the map vote offers arenas of the same group on other servers.
        for (ServerSelector.Candidate c : servers.candidates(now)) platform.sendToServer(c.serverId, msg);
    }

    /** A plain message to a player (used by the platform's commands). */
    public void platformMessage(UUID player, String key) {
        platform.message(player, key, Map.of());
    }

    int pendingCount() {
        return byRequest.size();
    }
}
