package com.l299l.newbedwars.network;

import com.l299l.newbedwars.api.view.NetworkMode;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.group.ArenaGroupStore;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.config.data.IOExecutor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Arena groups in network mode. Lobby servers own them ({@code /bw group} there, entries {@code arena} or
 * {@code server:arena}) and send them to the proxy after every edit and on each player join; the proxy hands the
 * newest set to every server. Game servers keep the last set in {@code data/network-groups.json} and use the
 * entries that mean their own arenas.
 */
final class NetworkGroupSync {
    static final String CACHE_FILE = "network-groups.json";

    private final NetworkMode mode;
    private final String serverId;
    private final Messenger messenger;
    private final Path dataDir;
    private final Logger log;
    private final Runnable onApplied;
    /** GAME: revision of the groups in use (-1 = none yet). */
    private long revision = -1;

    NetworkGroupSync(NetworkMode mode, String serverId, Messenger messenger, Path dataDir, Logger log, Runnable onApplied) {
        this.mode = mode;
        this.serverId = serverId;
        this.messenger = messenger;
        this.dataDir = dataDir;
        this.log = log;
        this.onApplied = onApplied;
    }

    void start() {
        ArenaGroupManager groups = ArenaGroupManager.get();
        if (groups == null) return;
        if (mode == NetworkMode.LOBBY) {
            groups.setChangeListener(this::sendNow);
            return;
        }
        if (!groups.getGroups().isEmpty()) {
            log.info("[Network] data/groups.json of this game server is not used: arena groups are set on the lobby server (/bw group there).");
        }
        List<ArenaGroup> cached = List.of();
        try {
            ArenaGroupStore.Loaded loaded = ArenaGroupStore.read(cacheFile());
            cached = loaded.groups();
            if (!cached.isEmpty() || loaded.revision() > 0) revision = loaded.revision();
        } catch (IOException | RuntimeException e) {
            log.warning("[Network] Could not read data/" + CACHE_FILE + ": " + e.getMessage());
        }
        groups.useNetwork(forServer(cached, serverId));
    }

    void stop() {
        ArenaGroupManager groups = ArenaGroupManager.get();
        if (groups != null && mode == NetworkMode.LOBBY) groups.setChangeListener(null);
    }

    /** LOBBY: tell the proxy our groups through {@code via} (it keeps the newest). */
    void sendVia(Player via) {
        NetworkGroups payload = payload();
        if (mode == NetworkMode.LOBBY && payload != null) messenger.send(via, NetMessage.of(serverId, payload));
    }

    private void sendNow() {
        NetworkGroups payload = payload();
        if (payload != null) messenger.send(NetMessage.of(serverId, payload));
    }

    private static NetworkGroups payload() {
        ArenaGroupManager groups = ArenaGroupManager.get();
        return groups == null ? null : toPayload(groups.getGroups(), groups.getRevision());
    }

    /** Groups from the proxy. */
    void onGroups(NetworkGroups g) {
        ArenaGroupManager groups = ArenaGroupManager.get();
        if (g == null || groups == null) return;
        List<ArenaGroup> list = fromPayload(g);
        if (mode == NetworkMode.LOBBY) {
            if (g.revision <= groups.getRevision()) return;
            groups.replace(list, g.revision);
            log.info("[Network] Arena groups updated from another lobby server.");
            return;
        }
        if (g.revision <= revision) return;
        revision = g.revision;
        Path file = cacheFile();
        long rev = g.revision;
        IOExecutor.submit(() -> {
            try {
                ArenaGroupStore.save(file, list, rev, null, 0);
            } catch (IOException e) {
                log.warning("[Network] Could not save data/" + CACHE_FILE + ": " + e.getMessage());
            }
        });
        groups.useNetwork(forServer(list, serverId));
        onApplied.run();
    }

    private Path cacheFile() {
        return dataDir.resolve("data").resolve(CACHE_FILE);
    }

    static NetworkGroups toPayload(Iterable<ArenaGroup> groups, long revision) {
        List<NetworkGroups.Group> out = new ArrayList<>();
        for (ArenaGroup g : groups) {
            out.add(new NetworkGroups.Group(g.getName(), g.getDisplayItem(), new ArrayList<>(g.getArenas())));
        }
        return new NetworkGroups(revision, out);
    }

    static List<ArenaGroup> fromPayload(NetworkGroups g) {
        List<ArenaGroup> out = new ArrayList<>();
        if (g.groups == null) return out;
        for (NetworkGroups.Group n : g.groups) {
            if (n == null || n.name == null || n.name.isBlank()) continue;
            ArenaGroup group = new ArenaGroup(n.name, n.displayItem);
            if (n.arenas != null) for (String a : n.arenas) if (a != null && !a.isBlank()) group.add(a);
            out.add(group);
        }
        return out;
    }

    /**
     * The groups as a game server sees them: only entries for its own arenas ({@code arena} or
     * {@code thisServer:arena}), as plain arena names; an arena stays in the first group that has it.
     */
    static List<ArenaGroup> forServer(List<ArenaGroup> all, String serverId) {
        List<ArenaGroup> out = new ArrayList<>();
        Set<String> taken = new HashSet<>();
        for (ArenaGroup g : all) {
            ArenaGroup local = new ArenaGroup(g.getName(), g.getDisplayItem());
            for (String entry : g.getArenas()) {
                String arena = NetworkGroups.arenaOf(entry);
                if (!NetworkGroups.covers(entry, serverId, arena)) continue;
                if (taken.add(arena.toLowerCase(Locale.ROOT))) local.add(arena);
            }
            out.add(local);
        }
        return out;
    }
}
