package com.l299l.newbedwars.arena;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Timers of players who left a running game and may still rejoin it. */
class RejoinManager {
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    /** When each timer runs out (epoch millis). */
    private final Map<UUID, Long> expiresAt = new HashMap<>();

    /** Starts (or restarts) the player's timer; {@code onExpire} runs when it is over. */
    void schedule(UUID id, long ticks, Runnable onExpire) {
        cancel(id);
        expiresAt.put(id, System.currentTimeMillis() + ticks * 50L);
        tasks.put(id, Bukkit.getScheduler().runTaskLater(NewBedwars.plugin, () -> {
            tasks.remove(id);
            expiresAt.remove(id);
            onExpire.run();
        }, ticks));
    }

    void cancel(UUID id) {
        expiresAt.remove(id);
        BukkitTask task = tasks.remove(id);
        if (task != null) task.cancel();
    }

    void cancelAll() {
        for (UUID id : waiting()) cancel(id);
    }

    /** Players whose timer is running (a copy). */
    List<UUID> waiting() {
        return new ArrayList<>(tasks.keySet());
    }

    /** Waiting players and when their time is over (epoch millis), a copy. */
    Map<UUID, Long> slots() {
        return new HashMap<>(expiresAt);
    }
}
