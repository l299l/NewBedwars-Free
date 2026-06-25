package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.team.Team;
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

    private static final long DESPAWN_SECONDS = 180;
    private static final long TARGET_INTERVAL = 40L; // 2 s

    private static final Map<String, BukkitTask> golemTasks = new HashMap<>();

    @Override
    public void perform(Player player, IArena arena) {
        Team team = arena.getTeam(player);
        if (team == null) return;

        String pdcKey = arena.getArenaName() + ":" + team.getName();

        Location spawnLoc = player.getLocation();
        IronGolem golem = (IronGolem) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
        String golemUuid = golem.getUniqueId().toString();

        golem.setCustomNameVisible(true);
        golem.setRemoveWhenFarAway(false);
        golem.getPersistentDataContainer().set(TEAM_GOLEM_KEY, PersistentDataType.STRING, pdcKey);
        golem.getPersistentDataContainer().set(GOLEM_OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

        golem.setCustomName(golemName(team, DESPAWN_SECONDS));

        new BukkitRunnable() {
            long remaining = DESPAWN_SECONDS;
            @Override
            public void run() {
                if (!golem.isValid()) { cancel(); return; }
                remaining--;
                golem.setCustomName(golemName(team, remaining));
            }
        }.runTaskTimer(NewBedwars.plugin, 20L, 20L);

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

        new BukkitRunnable() {
            @Override public void run() { cleanupGolem(golemUuid); }
        }.runTaskLater(NewBedwars.plugin, DESPAWN_SECONDS * 20L);
    }

    private static String golemName(Team team, long seconds) {
        String timeColor;
        if (seconds > 90) timeColor = ChatColor.GREEN.toString();
        else if (seconds > 30) timeColor = ChatColor.YELLOW.toString();
        else timeColor = ChatColor.RED.toString();
        return team.getColor() + team.getName() + " Golem "
                + ChatColor.DARK_GRAY + "[" + timeColor + seconds + "s" + ChatColor.DARK_GRAY + "]";
    }

    public static void cleanupGolem(String golemUuid) {
        BukkitTask t = golemTasks.remove(golemUuid);
        if (t != null && !t.isCancelled()) t.cancel();
        try {
            Entity e = Bukkit.getEntity(UUID.fromString(golemUuid));
            if (e != null && e.isValid()) e.remove();
        } catch (IllegalArgumentException ignored) {}
    }

    public static void cleanupAllForArena(String arenaName) {
        String prefix = arenaName + ":";
        new ArrayList<>(golemTasks.keySet()).forEach(uuid -> {
            try {
                Entity e = Bukkit.getEntity(UUID.fromString(uuid));
                if (e == null) { golemTasks.remove(uuid); return; }
                String key = getGolemKey(e);
                if (key != null && key.startsWith(prefix)) cleanupGolem(uuid);
            } catch (IllegalArgumentException ignored) {}
        });
    }

    public static String getGolemKey(Entity entity) {
        return entity.getPersistentDataContainer().get(TEAM_GOLEM_KEY, PersistentDataType.STRING);
    }

    @Override
    public LogicType getType() { return LogicType.IRON_GOLEM; }
}
