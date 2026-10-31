package com.l299l.newbedwars.network;

import com.l299l.newbedwars.api.events.*;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.NetCodec;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.BooleanSupplier;

/**
 * GAME mode: sends this server's arena list to the proxy when something changes (debounced to one send per
 * tick) and every {@code StatePublishSeconds}; also writes it to MySQL when enabled (on change and every heartbeat).
 */
public final class ArenaStatePublisher implements Listener {
    private final Plugin plugin;
    private final Messenger messenger;
    private final String serverId;
    private final BooleanSupplier restarting;
    private final NetworkDatabase database;
    private final int publishSeconds;
    private final int heartbeatSeconds;
    private boolean scheduled;
    private String lastJson;
    private long lastDbWrite;
    private BukkitTask task;

    public ArenaStatePublisher(Plugin plugin, Messenger messenger, String serverId, BooleanSupplier restarting,
                               NetworkDatabase database, int publishSeconds, int heartbeatSeconds) {
        this.plugin = plugin;
        this.messenger = messenger;
        this.serverId = serverId;
        this.restarting = restarting;
        this.database = database;
        this.publishSeconds = publishSeconds;
        this.heartbeatSeconds = heartbeatSeconds;
    }

    public void start() {
        long period = 20L * Math.min(publishSeconds, database != null ? heartbeatSeconds : publishSeconds);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> publish(true), 40L, Math.max(20L, period));
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    public ServerState snapshot() {
        return ArenaStateSnapshot.build(serverId, ArenaRegistry.all(), a -> {
            ArenaGroupManager m = ArenaGroupManager.get();
            ArenaGroup g = m == null ? null : m.groupOf(a);
            return g == null ? null : g.getName();
        }, restarting.getAsBoolean(), System.currentTimeMillis());
    }

    /** Publishes on the next tick (several changes in one tick give one message). */
    public void changed() {
        if (scheduled) return;
        scheduled = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            scheduled = false;
            publish(false);
        });
    }

    private long lastSend;

    void publish(boolean periodic) {
        ServerState state = snapshot();
        String json = NetCodec.payloadToJson(new ServerState(state.serverId, state.mode, state.restarting, 0, state.arenas));
        boolean changed = !json.equals(lastJson);
        lastJson = json;
        long now = System.currentTimeMillis();
        if (changed || now - lastSend >= publishSeconds * 1000L - 50) {
            if (messenger.send(NetMessage.of(serverId, state))) lastSend = now;
        }
        if (database != null && (changed || now - lastDbWrite >= heartbeatSeconds * 1000L - 50)) {
            lastDbWrite = now;
            database.publish(state);
        }
    }

    // Everything that changes an arena's state or player count.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinArena(PlayerJoinArenaEvent e) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeaveArena(PlayerLeaveArenaEvent e) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStart(GameStartEvent e) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(GameEndEvent e) { changed(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReset(ArenaResetEvent e) { changed(); }

    /** The proxy only hears from this server while someone is online: send right after a join. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            lastJson = null;
            publish(false);
        }, 10L);
    }
}
