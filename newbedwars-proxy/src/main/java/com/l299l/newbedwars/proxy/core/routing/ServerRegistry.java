package com.l299l.newbedwars.proxy.core.routing;

import com.l299l.newbedwars.common.protocol.Limits;
import com.l299l.newbedwars.common.protocol.StateFit;
import com.l299l.newbedwars.common.routing.ServerSelector;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.common.protocol.payload.NetworkState;
import com.l299l.newbedwars.common.protocol.payload.RejoinSlot;
import com.l299l.newbedwars.common.protocol.payload.ServerState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The last known state of every game server. A state is fresh for {@code staleMillis} after it arrived
 * (plugin message) or after its MySQL heartbeat. With configured game servers only those are accepted;
 * without any, servers are learned from what they send. Not thread-safe; the platform layer serialises access.
 */
public final class ServerRegistry {
    private final Set<String> configuredGames;
    private final Set<String> lobbies;
    private final long staleMillis;
    private final Map<String, Entry> entries = new HashMap<>();
    /** The {@code time} of the last database row seen per server (the game server's clock); survives pruning. */
    private final Map<String, Long> databaseHeartbeats = new HashMap<>();
    private final Set<String> rejected = new HashSet<>();
    private Consumer<String> log = text -> {};
    private boolean warnedRejoinDropped;
    private boolean warnedTruncated;
    private boolean dirty;
    /** The network's groups from the lobby; when known they decide each arena's group (not the game server). */
    private NetworkGroups groups;

    private static final class Entry {
        ServerState state;
        /** Proxy clock, millis. */
        long receivedAt;
        boolean fromMessage;
        /** Counts replacements of {@link #state}, so a reservation can tell that a fresh state already includes it. */
        long version;
    }

    /** Players counted into an arena ahead of the next state; {@link #release} takes them out again. */
    public static final class Reservation {
        final String server;
        final String arena;
        final long version;
        int players;

        private Reservation(String server, String arena, long version, int players) {
            this.server = server;
            this.arena = arena;
            this.version = version;
            this.players = players;
        }
    }

    public ServerRegistry(Collection<String> gameServers, Collection<String> lobbyServers, long staleMillis) {
        this.configuredGames = new LinkedHashSet<>(gameServers);
        this.lobbies = new LinkedHashSet<>(lobbyServers);
        this.staleMillis = staleMillis;
    }

    public void setLogger(Consumer<String> log) {
        this.log = log;
    }

    public Set<String> lobbyServers() {
        return lobbies;
    }

    /** Configured game servers and every server that sent a state. */
    public Set<String> gameServers() {
        Set<String> out = new LinkedHashSet<>(configuredGames);
        out.addAll(entries.keySet());
        out.removeAll(lobbies);
        return out;
    }

    public boolean isLobby(String server) {
        return server != null && lobbies.contains(server);
    }

    /** A configured game server, or any server that sent a state. */
    public boolean isGame(String server) {
        return server != null && (configuredGames.contains(server) || entries.containsKey(server));
    }

    /** A plugin message state from {@code server}. The sender's id is trusted over the payload's. */
    public void update(String server, ServerState state, long now) {
        if (server == null || state == null || isLobby(server)) return;
        Entry e = entryFor(server);
        if (e == null) return;
        ServerState copy = copy(server, state);
        label(copy);
        e.state = copy;
        e.version++;
        e.receivedAt = now;
        e.fromMessage = true;
        dirty = true;
    }

    /**
     * New groups from a lobby: relabel every known arena right away, so routing and lobbies see them even on
     * game servers nobody is on (those get the groups themselves when a player arrives).
     */
    public void setGroups(NetworkGroups groups) {
        this.groups = groups;
        for (Entry e : entries.values()) label(e.state);
        dirty = true;
    }

    /** The entry of {@code server}, created when the server is allowed to report; null when it is not. */
    private Entry entryFor(String server) {
        Entry e = entries.get(server);
        if (e != null) return e;
        boolean allowed = configuredGames.isEmpty() ? entries.size() < Limits.MAX_SERVERS : configuredGames.contains(server);
        if (!allowed || server.length() > Limits.MAX_NAME_LENGTH) {
            if (rejected.size() < Limits.MAX_SERVERS * 4 && rejected.add(server)) {
                log.accept("Ignoring state of '" + (server.length() > 32 ? server.substring(0, 32) + "..." : server)
                        + "': not one of the configured game servers");
            }
            return null;
        }
        e = new Entry();
        entries.put(server, e);
        return e;
    }

    private void label(ServerState state) {
        if (groups == null || state == null) return;
        for (ArenaInfo a : state.arenas) a.group = groups.groupOf(state.serverId, a.name);
    }

    /**
     * States read from MySQL. Their {@code time} is the game server's clock, so it is only compared with the
     * previous row of the same server: a changed heartbeat means the server is alive at {@code now} (proxy clock).
     * A state that came by plugin message and is still fresh is never replaced.
     */
    public void mergeFromDatabase(List<ServerState> states, long now) {
        if (states == null) return;
        for (ServerState s : states) {
            if (s == null || s.serverId == null || isLobby(s.serverId)) continue;
            Long seen = databaseHeartbeats.get(s.serverId);
            if (seen != null && seen == s.time) continue;
            Entry e = entryFor(s.serverId);
            if (e == null) continue;
            databaseHeartbeats.put(s.serverId, s.time);
            if (e.state != null && e.fromMessage && now - e.receivedAt <= staleMillis) continue;
            ServerState copy = copy(s.serverId, s);
            label(copy);
            e.state = copy;
            e.version++;
            e.receivedAt = now;
            e.fromMessage = false;
            dirty = true;
        }
    }

    private static ServerState copy(String serverId, ServerState s) {
        List<ArenaInfo> arenas = new ArrayList<>();
        if (s.arenas != null) {
            for (ArenaInfo a : s.arenas) if (a != null && a.name != null) arenas.add(copy(a));
        }
        return new ServerState(serverId, s.mode, s.restarting, s.time, arenas);
    }

    private static ArenaInfo copy(ArenaInfo a) {
        ArenaInfo c = new ArenaInfo(a.name, a.group, a.state, a.players, a.maxPlayers, a.maxInTeam, a.teams, a.allowParties);
        if (a.rejoin != null) {
            for (RejoinSlot r : a.rejoin) if (r != null) c.rejoin.add(new RejoinSlot(r.player, r.expiresAt));
        }
        return c;
    }

    public boolean isFresh(String server, long now) {
        Entry e = entries.get(server);
        return e != null && e.state != null && now - e.receivedAt <= staleMillis;
    }

    /** All game servers for {@link ServerSelector}. */
    public List<ServerSelector.Candidate> candidates(long now) {
        Set<String> ids = new LinkedHashSet<>(configuredGames);
        ids.addAll(entries.keySet());
        List<ServerSelector.Candidate> out = new ArrayList<>();
        for (String id : ids) {
            Entry e = entries.get(id);
            out.add(new ServerSelector.Candidate(id, e == null ? null : e.state, isFresh(id, now)));
        }
        return out;
    }

    /** Forgets servers that have been silent for ten stale windows. */
    public void prune(long now) {
        for (Iterator<Entry> it = entries.values().iterator(); it.hasNext(); ) {
            Entry e = it.next();
            if (now - e.receivedAt > staleMillis * 10) {
                it.remove();
                dirty = true;
            }
        }
    }

    /** What lobbies get: copies of the fresh servers, sorted by id, shrunk to fit one plugin message. */
    public NetworkState snapshot(long now) {
        List<ServerState> list = new ArrayList<>();
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            if (isFresh(e.getKey(), now)) list.add(copy(e.getKey(), e.getValue().state));
        }
        list.sort((a, b) -> a.serverId.compareToIgnoreCase(b.serverId));
        NetworkState out = new NetworkState(list);
        StateFit.Result fitted = StateFit.fit(out, Limits.MAX_PAYLOAD_BYTES);
        if (fitted == StateFit.Result.REJOIN_DROPPED && !warnedRejoinDropped) {
            warnedRejoinDropped = true;
            log.accept("The network state is too big for one message: rejoin data is left out");
        } else if (fitted == StateFit.Result.ARENAS_TRUNCATED && !warnedTruncated) {
            warnedTruncated = true;
            log.accept("The network state is too big for one message: the least useful arenas are left out");
        }
        return out;
    }

    /** Counts {@code n} players into an arena right away, so the next request does not pick a full one. */
    public Reservation reserve(String server, String arena, int n) {
        Entry e = server == null ? null : entries.get(server);
        if (e == null || e.state == null || arena == null) return null;
        for (ArenaInfo a : e.state.arenas) {
            if (a.name.equalsIgnoreCase(arena)) {
                a.players += n;
                dirty = true;
                return new Reservation(server, a.name, e.version, n);
            }
        }
        return null;
    }

    /**
     * Takes {@code n} of the reserved players out of the arena again (the join was cancelled, failed or is over).
     * Does nothing once a newer state replaced the one that was reserved on: it already counts the real players.
     */
    public void release(Reservation r, int n) {
        if (r == null || n <= 0) return;
        int count = Math.min(n, r.players);
        r.players -= count;
        Entry e = entries.get(r.server);
        if (count <= 0 || e == null || e.state == null || e.version != r.version) return;
        for (ArenaInfo a : e.state.arenas) {
            if (a.name.equalsIgnoreCase(r.arena)) {
                a.players = Math.max(0, a.players - count);
                dirty = true;
                return;
            }
        }
    }

    /** True once after something changed (to push the state to lobbies). */
    public boolean takeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    /** Fresh/stale changes over time need a push too. */
    public void markDirty() {
        dirty = true;
    }
}
