package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.dragon.DragonController;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

final class GameplayUtil {
    private GameplayUtil() {}

    record MobKey(String arenaName, String teamName) {

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

    static Player playerDamager(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    static Team dragonTeam(IArena arena, EntityDamageEvent damage) {
        if (!(damage instanceof EntityDamageByEntityEvent byEntity)) return null;
        MobKey key = MobKey.parse(DragonController.ownerOf(byEntity.getDamager()));
        if (key == null || !key.arenaName().equals(arena.getArenaName())) return null;
        return arena.getTeam(key.teamName());
    }

    static CustomItem findCustomItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getItemMeta() == null) return null;
        NamespacedKey key = new NamespacedKey(NewBedwars.plugin, "custom_item_name");
        String name = stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (name == null) return null;
        return NewBedwars.plugin.getCustomItemManager().getCustomItem(name);
    }

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
