package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;

public class SetupListener implements Listener {

    private static IArena setupArena(Player p) {
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || !arena.getPlayersOnSetup().contains(p.getUniqueId())) return null;
        return arena;
    }

    private static void backToSetup(Player p, IArena arena) {
        p.setFallDistance(0f);
        p.setVelocity(new Vector(0, 0, 0));
        p.setFireTicks(0);
        p.teleport(arena.getSetupSpawn());
        p.setGameMode(GameMode.CREATIVE);
        p.setAllowFlight(true);
        p.setFlying(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        IArena arena = setupArena(p);
        if (arena == null || e.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        e.setCancelled(true);
        backToSetup(p, arena);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (setupArena(p) == null) return;
        e.setDeathMessage(null);
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        IArena arena = setupArena(p);
        if (arena == null) return;
        e.setRespawnLocation(arena.getSetupSpawn());
        Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
            if (p.isOnline() && setupArena(p) == arena) backToSetup(p, arena);
        });
    }
}
