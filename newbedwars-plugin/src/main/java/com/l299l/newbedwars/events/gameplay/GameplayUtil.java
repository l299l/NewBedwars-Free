package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Small helpers shared by the gameplay listeners. */
final class GameplayUtil {
    private GameplayUtil() {}

    /** The arena and team name of a golem/silverfish key {@code "arena:team"}. */
    record MobKey(String arenaName, String teamName) {
        /** @return null when the key is null or has no colon */
        static MobKey parse(String key) {
            if (key == null) return null;
            int colon = key.indexOf(':');
            if (colon < 0) return null;
            return new MobKey(key.substring(0, colon), key.substring(colon + 1));
        }

        IArena arena() {
            return ArenaRegistry.byName(arenaName);
        }
    }

    /** The player behind a hit: the damager itself or the shooter of a projectile; null for anything else. */
    static Player playerDamager(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    /** The shop item an item stack was bought as, or null. */
    static CustomItem findCustomItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getItemMeta() == null) return null;
        NamespacedKey key = new NamespacedKey(NewBedwars.plugin, "custom_item_name");
        String name = stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (name == null) return null;
        return NewBedwars.plugin.getCustomItemManager().getCustomItem(name);
    }

    /** The Y below which spectators and waiting players are rescued: the Quick Void Y, else the bottom of the world. */
    static double safeVoidY(IArena arena, World world) {
        Integer voidY = arena.getQuickVoidY();
        return voidY != null ? voidY : world.getMinHeight();
    }

    static void rescueFromVoid(Player p, IArena arena) {
        Location spawn = arena.getWaitingSpawn();
        if (spawn == null) return;
        p.setFallDistance(0f);
        p.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        p.teleport(spawn);
        if (arena.getSpectators().contains(p)) {
            p.setAllowFlight(true);
            p.setFlying(true);
        }
    }
}
