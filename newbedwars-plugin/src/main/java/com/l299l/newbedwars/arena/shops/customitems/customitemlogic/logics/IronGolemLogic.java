package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IronGolemLogic implements CustomLogic {

    public static final NamespacedKey TEAM_GOLEM_KEY =
            new NamespacedKey(NewBedwars.plugin, "team_golem");
    public static final NamespacedKey GOLEM_OWNER_KEY =
            new NamespacedKey(NewBedwars.plugin, "team_golem_owner");

    private static final long TARGET_INTERVAL = 40L; // 2 s

    private static final Map<String, BukkitTask> golemTasks = new HashMap<>();

    // Track active golem per team: "arenaName:teamName" -> golemUuid
    private static final Map<String, String> teamActiveGolem = new HashMap<>();

    @Override
    public boolean perform(Player player, IArena arena) {
        Team team = arena.getTeam(player);
        if (team == null) return false;

        String pdcKey = arena.getArenaName() + ":" + team.getName();

        // Check spawn limit unless multiple golems are explicitly allowed
        if (!allowsMultipleGolems(arena) && hasActiveGolem(pdcKey)) {
            NewBedwars.plugin.getMessages().send(player, "GolemAlreadyActive");
            return false; // Spawn blocked - item should NOT be consumed
        }

        long lifetime = getGolemLifetime();

        Location spawnLoc = player.getLocation();
        IronGolem golem = (IronGolem) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
        String golemUuid = golem.getUniqueId().toString();

        golem.setCustomNameVisible(true);
        golem.setRemoveWhenFarAway(false);
        golem.getPersistentDataContainer().set(TEAM_GOLEM_KEY, PersistentDataType.STRING, pdcKey);
        golem.getPersistentDataContainer().set(GOLEM_OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

        golem.setCustomName(golemName(team, lifetime));

        // Countdown name updater
        new BukkitRunnable() {
            long remaining = lifetime;
            @Override
            public void run() {
                if (!golem.isValid()) { cancel(); return; }
                remaining--;
                golem.setCustomName(golemName(team, remaining));
            }
        }.runTaskTimer(NewBedwars.plugin, 20L, 20L);

        // Auto-target nearest enemy
        BukkitTask targetTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!golem.isValid()) { cleanupGolem(golemUuid); cancel(); return; }
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Team t : arena.getTeams().values()) {
                    if (t.equals(team)) continue;
                    for (Player p : t.getPlayers()) {
                        if (!p.isOnline() || !p.getWorld().equals(golem.getWorld())) continue;
                        double dist = p.getLocation().distanceSquared(golem.getLocation());
                        if (dist < nearestDist) { nearestDist = dist; nearest = p; }
                    }
                }
                if (nearest != null) ((Mob) golem).setTarget(nearest);
            }
        }.runTaskTimer(NewBedwars.plugin, TARGET_INTERVAL, TARGET_INTERVAL);
        golemTasks.put(golemUuid, targetTask);

        // Register as team's active golem (when limit is enforced)
        if (!allowsMultipleGolems(arena)) {
            teamActiveGolem.put(pdcKey, golemUuid);
        }

        // Auto-despawn after configured lifetime
        new BukkitRunnable() {
            @Override public void run() { cleanupGolem(golemUuid); }
        }.runTaskLater(NewBedwars.plugin, lifetime * 20L);
        
        return true; // Golem successfully spawned
    }

    private static String golemName(Team team, long seconds) {
        String timeColor;
        if (seconds > 90) timeColor = ChatColor.GREEN.toString();
        else if (seconds > 30) timeColor = ChatColor.YELLOW.toString();
        else timeColor = ChatColor.RED.toString();
        return team.getColor() + team.getName() + " Golem "
                + ChatColor.DARK_GRAY + "[" + timeColor + seconds + "s" + ChatColor.DARK_GRAY + "]";
    }

    /**
     * Check if a team already has a living active golem.
     */
    private static boolean hasActiveGolem(String teamKey) {
        String golemUuid = teamActiveGolem.get(teamKey);
        if (golemUuid == null) return false;

        try {
            Entity e = Bukkit.getEntity(UUID.fromString(golemUuid));
            if (e == null || !e.isValid()) {
                // Stale reference — entity already dead or removed
                teamActiveGolem.remove(teamKey);
                return false;
            }
            return true;
        } catch (IllegalArgumentException ex) {
            teamActiveGolem.remove(teamKey);
            return false;
        }
    }

    /**
     * Get configured golem lifetime in seconds; defaults to 120.
     */
    private static long getGolemLifetime() {
        Integer configured = Properties.IronGolemLifetime;
        return (configured != null && configured > 0) ? configured.longValue() : 120L;
    }

    /**
     * Check whether the arena allows multiple golems per team.
     * Uses the per-arena gamerule with fallback to the global default.
     */
    private static boolean allowsMultipleGolems(IArena arena) {
        if (arena.getGamerules() != null && arena.getGamerules().AllowMultipleGolems != null) {
            return arena.getGamerules().AllowMultipleGolems;
        }
        return false;
    }

    public static void cleanupGolem(String golemUuid) {
        // Cancel targeting task
        BukkitTask t = golemTasks.remove(golemUuid);
        if (t != null && !t.isCancelled()) t.cancel();

        // Remove from team tracking
        teamActiveGolem.values().remove(golemUuid);

        // Remove entity
        try {
            Entity e = Bukkit.getEntity(UUID.fromString(golemUuid));
            if (e != null && e.isValid()) e.remove();
        } catch (IllegalArgumentException ignored) {}
    }

    public static void cleanupAllForArena(String arenaName) {
        String prefix = arenaName + ":";

        // Cleanup golem entities and tasks
        new ArrayList<>(golemTasks.keySet()).forEach(uuid -> {
            try {
                Entity e = Bukkit.getEntity(UUID.fromString(uuid));
                if (e == null) {
                    golemTasks.remove(uuid);
                    return;
                }
                String key = getGolemKey(e);
                if (key != null && key.startsWith(prefix)) {
                    cleanupGolem(uuid);
                }
            } catch (IllegalArgumentException ignored) {}
        });

        // Cleanup team tracking entries for this arena
        teamActiveGolem.keySet().removeIf(teamKey -> teamKey.startsWith(prefix));
    }

    public static String getGolemKey(Entity entity) {
        return entity.getPersistentDataContainer().get(TEAM_GOLEM_KEY, PersistentDataType.STRING);
    }

    @Override
    public LogicType getType() { return LogicType.IRON_GOLEM; }
}
