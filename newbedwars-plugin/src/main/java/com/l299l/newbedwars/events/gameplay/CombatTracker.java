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

public class CombatTracker {

    private static final long ATTACKER_TICKS = 200L;

    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, BukkitTask> clearTasks = new HashMap<>();

    private final Set<UUID> lastHitByMob = new HashSet<>();
    private final Set<UUID> quickVoidDeaths = new HashSet<>();
    private final Map<UUID, Location> lastGround = new HashMap<>();

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

    public boolean takeMobHit(UUID victimId) {
        return lastHitByMob.remove(victimId);
    }

    public void markVoidDeath(UUID id) {
        quickVoidDeaths.add(id);
    }

    public boolean takeVoidDeath(UUID id) {
        return quickVoidDeaths.remove(id);
    }

    public void setGround(UUID id, Location location) {
        lastGround.put(id, location);
    }

    public Location takeGround(UUID id) {
        return lastGround.remove(id);
    }

    public void forget(UUID id) {
        lastAttacker.remove(id);
        lastHitByMob.remove(id);
        BukkitTask clearTask = clearTasks.remove(id);
        if (clearTask != null) clearTask.cancel();
        quickVoidDeaths.remove(id);
        lastGround.remove(id);
    }
}
