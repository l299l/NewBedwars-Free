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


public final class ServerRegistry {
    private final Set<String> configuredGames;
    private final Set<String> lobbies;
    private final long staleMillis;
    private final Map<String, Entry> entries = new HashMap<>();
    private final Map<String, Long> databaseHeartbeats = new HashMap<>();
    private final Set<String> rejected = new HashSet<>();
    private Consumer<String> log = text -> {};
    private boolean warnedRejoinDropped;
    private boolean warnedTruncated;
    private boolean dirty;
    private NetworkGroups groups;

    private static final class Entry {
        ServerState state;
        long receivedAt;
        boolean fromMessage;
        long version;
    }

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

    public Set<String> gameServers() {
        Set<String> out = new LinkedHashSet<>(configuredGames);
        out.addAll(entries.keySet());
        out.removeAll(lobbies);
        return out;
    }

    public boolean isLobby(String server) {
        return server != null && lobbies.contains(server);
    }

    public boolean isGame(String server) {
        return server != null && (configuredGames.contains(server) || entries.containsKey(server));
    }

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

    public void setGroups(NetworkGroups groups) {
        this.groups = groups;
        for (Entry e : entries.values()) label(e.state);
        dirty = true;
    }

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


    public void prune(long now) {
        for (Iterator<Entry> it = entries.values().iterator(); it.hasNext(); ) {
            Entry e = it.next();
            if (now - e.receivedAt > staleMillis * 10) {
                it.remove();
                dirty = true;
            }
        }
    }


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


    public boolean takeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    public void markDirty() {
        dirty = true;
    }
}
