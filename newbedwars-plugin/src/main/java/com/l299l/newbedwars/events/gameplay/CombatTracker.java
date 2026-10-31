package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-player combat state shared by the death, damage and movement listeners. */
public class CombatTracker {
    /** How long a hit counts for the kill (10 s). */
    private static final long ATTACKER_TICKS = 200L;

    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, BukkitTask> clearTasks = new HashMap<>();
    /** Victims whose last hit came from a golem/silverfish (resources drop instead of going to the owner). */
    private final Set<UUID> lastHitByMob = new HashSet<>();
    private final Set<UUID> quickVoidDeaths = new HashSet<>();
    private final Map<UUID, Location> lastGround = new HashMap<>();

    /** Remembers who hit the victim last; forgotten after {@link #ATTACKER_TICKS}. */
    public void record(UUID victimId, UUID attackerId, boolean byMob) {
        lastAttacker.put(victimId, attackerId);
        if (byMob) lastHitByMob.add(victimId);
        else lastHitByMob.remove(victimId);
        BukkitTask prev = clearTasks.remove(victimId);
        if (prev != null) prev.cancel();
        clearTasks.put(victimId, Bukkit.getScheduler().runTaskLater(NewBedwars.plugin, () -> {
            lastAttacker.remove(victimId);
            lastHitByMob.remove(victimId);
            clearTasks.remove(victimId);
        }, ATTACKER_TICKS));
    }

    public UUID lastAttacker(UUID victimId) {
        return lastAttacker.get(victimId);
    }

    /** True (once) when the victim's last hit came from a mob. */
    public boolean takeMobHit(UUID victimId) {
        return lastHitByMob.remove(victimId);
    }

    public void markVoidDeath(UUID id) {
        quickVoidDeaths.add(id);
    }

    /** True (once) when the player was killed by the Quick Void Y. */
    public boolean takeVoidDeath(UUID id) {
        return quickVoidDeaths.remove(id);
    }

    public void setGround(UUID id, Location location) {
        lastGround.put(id, location);
    }

    /** The last place the player stood on the ground (removed), or null. */
    public Location takeGround(UUID id) {
        return lastGround.remove(id);
    }

    /** Drops everything about a player who left the server. */
    public void forget(UUID id) {
        lastAttacker.remove(id);
        lastHitByMob.remove(id);
        BukkitTask clearTask = clearTasks.remove(id);
        if (clearTask != null) clearTask.cancel();
        quickVoidDeaths.remove(id);
        lastGround.remove(id);
    }
}
