package com.l299l.newbedwars.proxy.core.routing;

import com.l299l.newbedwars.proxy.core.ProxyPlatform;
import com.l299l.newbedwars.proxy.core.group.GroupRelay;
import com.l299l.newbedwars.proxy.core.party.PartyService;
import com.l299l.newbedwars.proxy.core.party.ProxyParty;

import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.routing.ServerSelector;
import com.l299l.newbedwars.common.protocol.payload.JoinReject;
import com.l299l.newbedwars.common.protocol.payload.JoinRequest;
import com.l299l.newbedwars.common.protocol.payload.JoinTicket;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.common.protocol.payload.SendToLobby;
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
import java.util.function.Consumer;

public final class Router {
    static final int MAX_TRIES = 2;
    static final long REQUEST_COOLDOWN_MILLIS = 2000L;

    private final ProxyPlatform platform;
    private final ServerRegistry servers;
    private final PartyService parties;
    private final GroupRelay groups;
    private final long ticketMillis;
    private Consumer<String> log = text -> {};
    private final Map<String, Pending> byRequest = new HashMap<>();
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
        String onlyServer;
        ServerSelector.Choice current;
        long expiresAt;
        final long createdAt;
        ServerRegistry.Reservation reservation;

        Pending(String id, List<UUID> players, String partyId, TargetKind kind, String target, long createdAt) {
            this.id = id;
            this.createdAt = createdAt;
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

    public void setLogger(Consumer<String> log) {
        this.log = log;
    }

    public ServerRegistry servers() {
        return servers;
    }

    public GroupRelay groups() {
        return groups;
    }

    public void handle(String server, UUID via, NetMessage msg, long now) {
        switch (msg.type()) {
            case HELLO -> onHello(server, via, now);
            case SERVER_STATE -> servers.update(server, msg.payload(ServerState.class), now);
            case JOIN_REQUEST -> onJoinRequest(server, via, msg.payload(JoinRequest.class), now);
            case JOIN_REJECT -> onReject(server, msg.payload(JoinReject.class), now);
            case GROUPS -> {
                if (servers.isLobby(server)
                        && groups.onGroups(server, msg.payload(NetworkGroups.class), servers.lobbyServers(), servers.gameServers(), now)) {
                    servers.setGroups(groups.current());
                }
            }
            case SEND_TO_LOBBY -> onSendToLobby(server, msg.payload(SendToLobby.class));
            default -> {
                // NETWORK_STATE, JOIN_TICKET and PARTY_SYNC only go from the proxy to backends.
            }
        }
    }

    private void onSendToLobby(String server, SendToLobby toLobby) {
        if (toLobby == null || toLobby.players == null) return;
        for (UUID p : toLobby.players) {
            if (p != null && server.equals(platform.currentServer(p))) sendToLobby(p);
        }
    }

    void onJoinRequest(String server, UUID via, JoinRequest r, long now) {
        if (r == null || r.player == null || !r.player.equals(via)) return;
        if (r.target != null && r.target.length() > ServerSelector.MAX_TARGET_LENGTH) return;
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
        boolean voteMove = r.players != null && !r.players.isEmpty();
        if (!voteMove) {
            Pending running = byPlayer.get(requester);
            if (running != null && now - running.createdAt < REQUEST_COOLDOWN_MILLIS) return;
        }
        if (voteMove) {
            if (!servers.isGame(server)) return;
            for (UUID u : r.players) {
                if (u != null && server.equals(platform.currentServer(u)) && !group.contains(u)) group.add(u);
            }
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
        Pending p = new Pending(id, group, partyId, r.kind == null ? TargetKind.ANY : r.kind, r.target, now);
        p.onlyServer = r.server;
        byRequest.put(p.id, p);
        for (UUID u : group) byPlayer.put(u, p);
        route(p, now);
    }

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

    private void onRejoinRequest(UUID player, long now) {
        ServerSelector.Choice where = ServerSelector.findRejoin(servers.candidates(now), player, now);
        if (where == null) {
            platform.message(player, "NetworkNoRejoin", Map.of());
            return;
        }
        routeDirect(player, where, TargetKind.REJOIN, now);
    }

    private void routeDirect(UUID player, ServerSelector.Choice where, TargetKind kind, long now) {
        cancel(List.of(player));
        Pending p = new Pending("r" + (++nextId) + "-" + Long.toHexString(now), new ArrayList<>(List.of(player)), null,
                kind, where.arena, now);
        byRequest.put(p.id, p);
        byPlayer.put(player, p);
        p.current = where;
        p.tried.add(where.serverId);
        p.expiresAt = now + ticketMillis;
        log.accept(kind + " " + p.id + " -> " + platform.nameOf(player) + " to " + where);
        if (where.serverId.equals(platform.currentServer(player))) {
            sendTicket(p, player, now);
            return;
        }
        connect(p, player, where.serverId);
    }

    private void route(Pending p, long now) {
        releaseReservation(p);
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
        if (!choice.isBlind()) p.reservation = servers.reserve(choice.serverId, choice.arena, p.players.size());
        for (UUID u : new ArrayList<>(p.players)) {
            if (choice.serverId.equals(platform.currentServer(u))) {
                sendTicket(p, u, now);
                continue;
            }
            connect(p, u, choice.serverId);
        }
    }

    private void connect(Pending p, UUID player, String server) {
        platform.connect(player, server, ok -> {
            if (!ok) {
                log.accept("Could not connect " + platform.nameOf(player) + " to " + server);
                onConnectFailed(p, server);
            }
        });
    }

    public void onServerConnected(UUID player, String server, long now) {
        parties.onServerSwitch(player);
        sendTicketIfArrived(player, server, now);
        if (servers.isLobby(server) || servers.isGame(server)) {
            platform.sendToPlayerServer(player, NetMessage.of(PartyService.FROM, servers.snapshot(now)));
        }
    }

    private void onHello(String server, UUID via, long now) {
        if (via == null) return;
        parties.onServerSwitch(via);
        sendTicketIfArrived(via, server, now);
        if (servers.isLobby(server) || servers.isGame(server)) {
            platform.sendToPlayerServer(via, NetMessage.of(PartyService.FROM, servers.snapshot(System.currentTimeMillis())));
            groups.onHello(via);
        }
    }

    private void sendTicketIfArrived(UUID player, String server, long now) {
        Pending p = byPlayer.get(player);
        if (p != null && p.current != null && server.equals(p.current.serverId)) sendTicket(p, player, now);
    }

    private void sendTicket(Pending p, UUID player, long now) {
        if (p.ticketsSent.add(player)) log.accept("Ticket " + p.id + " -> " + platform.nameOf(player) + " on " + p.current.serverId);
        JoinTicket t = new JoinTicket(p.id, player, p.kind, p.target, p.current.arena, p.partyId,
                new ArrayList<>(p.players), p.expiresAt, Math.max(1L, p.expiresAt - now));
        platform.sendToPlayerServer(player, NetMessage.of(PartyService.FROM, t));
    }

    void onReject(String server, JoinReject r, long now) {
        if (r == null) return;
        Pending p = byRequest.get(r.requestId);
        // Only the first reject of the current try counts (a party sends one per member).
        if (p == null || p.current == null || !server.equals(p.current.serverId)) return;
        log.accept("Join " + p.id + " rejected by " + server + ": " + r.reason);
        retryOrFail(p, now);
    }

    private void onConnectFailed(Pending p, String server) {
        if (p.current == null || !server.equals(p.current.serverId)) return;
        retryOrFail(p, System.currentTimeMillis());
    }

    private void retryOrFail(Pending p, long now) {
        p.current = null;
        releaseReservation(p);
        if (p.kind == TargetKind.REJOIN || p.kind == TargetKind.SPECTATE) {
            fail(p, p.kind == TargetKind.REJOIN ? "NetworkRejoinFailed" : "NetworkSpectateFailed");
            return;
        }
        route(p, now);
    }

    private void fail(Pending p, String key) {
        log.accept("Join " + p.id + " found no server (tried " + p.tried + ")");
        remove(p);
        for (UUID u : p.players) {
            platform.message(u, key, Map.of());
            if (!servers.isLobby(platform.currentServer(u))) sendToLobby(u);
        }
    }

    private void remove(Pending p) {
        releaseReservation(p);
        byRequest.remove(p.id);
        for (UUID u : p.players) byPlayer.remove(u, p);
    }

    private void cancel(List<UUID> players) {
        for (UUID u : players) {
            Pending old = byPlayer.get(u);
            if (old != null) remove(old);
        }
    }

    private void releaseReservation(Pending p) {
        servers.release(p.reservation, p.players.size());
        p.reservation = null;
    }

    public void onDisconnect(UUID player) {
        Pending p = byPlayer.remove(player);
        if (p == null) return;
        if (p.reservation != null) servers.release(p.reservation, 1);
        p.players.remove(player);
        if (p.players.isEmpty()) remove(p);
    }

    public void tick(long now) {
        for (Iterator<Pending> it = byRequest.values().iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.expiresAt > 0 && p.expiresAt + ticketMillis < now) {
                it.remove();
                releaseReservation(p);
                for (UUID u : p.players) byPlayer.remove(u, p);
            }
        }
        servers.prune(now);
    }

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
        if (best == null) return;
        String lobby = best;
        platform.connect(player, lobby, ok -> {
            if (!ok) log.accept("Could not send " + platform.nameOf(player) + " to lobby " + lobby);
        });
    }

    public void pushState(long now, boolean force) {
        if (!servers.takeDirty() && !force) return;
        NetMessage msg = NetMessage.of(PartyService.FROM, servers.snapshot(now));
        for (String lobby : servers.lobbyServers()) platform.sendToServer(lobby, msg);
        for (ServerSelector.Candidate c : servers.candidates(now)) platform.sendToServer(c.serverId, msg);
    }

    public void platformMessage(UUID player, String key) {
        platform.message(player, key, Map.of());
    }

    int pendingCount() {
        return byRequest.size();
    }
}
