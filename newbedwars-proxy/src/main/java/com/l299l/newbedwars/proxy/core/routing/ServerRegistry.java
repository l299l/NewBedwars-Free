package com.l299l.newbedwars.proxy.core.routing;

import com.l299l.newbedwars.common.routing.ServerSelector;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.common.protocol.payload.NetworkState;
import com.l299l.newbedwars.common.protocol.payload.ServerState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The last known state of every game server. A state is fresh for {@code staleMillis} after it arrived
 * (plugin message) or after its MySQL heartbeat. Not thread-safe; the platform layer serialises access.
 */
public final class ServerRegistry {
    private final Set<String> configuredGames;
    private final Set<String> lobbies;
    private final long staleMillis;
    private final Map<String, Entry> entries = new HashMap<>();
    private boolean dirty;
    /** The network's groups from the lobby; when known they decide each arena's group (not the game server). */
    private NetworkGroups groups;

    private static final class Entry {
        ServerState state;
        long receivedAt;
    }

    public ServerRegistry(Collection<String> gameServers, Collection<String> lobbyServers, long staleMillis) {
        this.configuredGames = new LinkedHashSet<>(gameServers);
        this.lobbies = new LinkedHashSet<>(lobbyServers);
        this.staleMillis = staleMillis;
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
        state.serverId = server;
        if (state.arenas == null) state.arenas = new ArrayList<>();
        Entry e = entries.computeIfAbsent(server, k -> new Entry());
        e.state = state;
        e.receivedAt = now;
        label(state);
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

    private void label(ServerState state) {
        if (groups == null || state == null || state.arenas == null) return;
        for (ArenaInfo a : state.arenas) {
            if (a != null && a.name != null) a.group = groups.groupOf(state.serverId, a.name);
        }
    }

    /** States read from MySQL ({@code time} = heartbeat); used when newer than what we have. */
    public void mergeFromDatabase(List<ServerState> states) {
        for (ServerState s : states) {
            if (s == null || s.serverId == null || isLobby(s.serverId)) continue;
            Entry e = entries.get(s.serverId);
            if (e != null && e.receivedAt >= s.time) continue;
            if (e == null) {
                e = new Entry();
                entries.put(s.serverId, e);
            }
            if (s.arenas == null) s.arenas = new ArrayList<>();
            e.state = s;
            e.receivedAt = s.time;
            label(s);
            dirty = true;
        }
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

    /** What lobbies get: fresh servers only. */
    public NetworkState snapshot(long now) {
        List<ServerState> list = new ArrayList<>();
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            if (isFresh(e.getKey(), now)) list.add(e.getValue().state);
        }
        list.sort((a, b) -> a.serverId.compareToIgnoreCase(b.serverId));
        return new NetworkState(list);
    }

    /** Counts {@code n} players into an arena right away, so the next request does not pick a full one. */
    public void reserve(String server, String arena, int n) {
        Entry e = entries.get(server);
        if (e == null || e.state == null || arena == null) return;
        for (ArenaInfo a : e.state.arenas) {
            if (a.name.equalsIgnoreCase(arena)) {
                a.players += n;
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
