package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SilverfishLogic implements CustomLogic {

    private static final int SWARM_SIZE = 5;
    private static final int DESPAWN_TICKS = 30 * 20;

    public static final NamespacedKey SILVERFISH_KEY = new NamespacedKey(NewBedwars.plugin, "sf_team");
    public static final NamespacedKey SF_OWNER_KEY = new NamespacedKey(NewBedwars.plugin, "sf_owner");

    public static final Set<UUID> activeSilverfish = new HashSet<>();

    @Override
    public void perform(Player player, IArena arena) {
        Team team = arena.getTeam(player);
        for (int i = 0; i < SWARM_SIZE; i++) {
            Silverfish sf = (Silverfish) player.getLocation().getWorld()
                    .spawnEntity(player.getLocation(), EntityType.SILVERFISH);
            sf.setCustomNameVisible(false);
            if (team != null) {
                sf.getPersistentDataContainer().set(SILVERFISH_KEY, PersistentDataType.STRING,
                        arena.getArenaName() + ":" + team.getName());
            }
            sf.getPersistentDataContainer().set(SF_OWNER_KEY, PersistentDataType.STRING,
                    player.getUniqueId().toString());
            activeSilverfish.add(sf.getUniqueId());

            UUID sfId = sf.getUniqueId();
            new BukkitRunnable() {
                @Override
                public void run() {
                    org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(sfId);
                    if (e != null && e.isValid()) e.remove();
                    activeSilverfish.remove(sfId);
                }
            }.runTaskLater(NewBedwars.plugin, DESPAWN_TICKS);
        }
    }

    public static String getSilverfishKey(org.bukkit.entity.Entity entity) {
        return entity.getPersistentDataContainer().get(SILVERFISH_KEY, PersistentDataType.STRING);
    }

    public static String getSfOwner(org.bukkit.entity.Entity entity) {
        return entity.getPersistentDataContainer().get(SF_OWNER_KEY, PersistentDataType.STRING);
    }

    public static void cleanupAllForArena(IArena arena) {
        activeSilverfish.removeIf(id -> {
            org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(id);
            if (e != null && e.isValid() && arena.getArenaWorld().equals(e.getWorld())) {
                e.remove();
                return true;
            }
            return false;
        });
    }

    @Override
    public LogicType getType() {
        return LogicType.SILVERFISH;
    }
}
