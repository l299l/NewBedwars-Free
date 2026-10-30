package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.*;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.configuration.game.guis.spectator.SpectatorEffectsGUI;
import com.l299l.newbedwars.gui.configuration.game.guis.spectator.SpectatorPlayersGUI;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.UUID;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

/** Hotbar items of waiting players and spectators (leave bed, compass, effects), and spectator protection. */
public class SpectatorListener implements Listener {
    private final NewBedwars plugin;
    private final HashMap<UUID, BukkitTask> pendingLeave = new HashMap<>();

    public SpectatorListener(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerQuitRequest(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (ArenaRegistry.byWorld(p.getWorld()) == null) return;
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (!arena.isPlayerInArena(p) && !arena.getSpectators().contains(p)) return;
        if (e.getHand() != null && e.getHand() != EquipmentSlot.HAND) return;
        Material held = p.getInventory().getItemInMainHand().getType();

        if (arena.isPlayerInArena(p) && (arena.status() == GameStatus.waiting || arena.status() == GameStatus.starting)) {
            if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (held.equals(plugin.getGuiManager().getLeaveItem(p).getType())) {
                e.setCancelled(true);
                handleBedLeave(p, arena, false);
            }
            return;
        }

        if ((arena.status() == GameStatus.playing || arena.status() == GameStatus.ending)
                && arena.getSpectators().contains(p)) {
            e.setCancelled(true);
            if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (held == Material.RED_BED) {
                handleBedLeave(p, arena, true);
            } else if (held == Material.COMPASS) {
                p.openInventory(new SpectatorPlayersGUI(plugin.getGuiManager(), p, arena).getInventory());
            } else if (held == Material.COMPARATOR) {
                p.openInventory(new SpectatorEffectsGUI(plugin.getGuiManager(), p).getInventory());
            }
        }
    }

    private void handleBedLeave(Player p, IArena arena, boolean isSpectator) {
        if (!Properties.BedLeaveConfirmEnabled) {
            doLeave(p, arena, isSpectator);
            return;
        }
        if (pendingLeave.containsKey(p.getUniqueId())) {
            pendingLeave.remove(p.getUniqueId()).cancel();
            plugin.getMessages().send(p, "BedLeaveCancelled");
            return;
        }
        int delay = Properties.BedLeaveConfirmDelay;
        int[] secondsLeft = {delay};
        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || (!arena.isPlayerInArena(p) && !arena.getSpectators().contains(p))) {
                    pendingLeave.remove(p.getUniqueId());
                    cancel();
                    return;
                }
                if (secondsLeft[0] <= 0) {
                    pendingLeave.remove(p.getUniqueId());
                    cancel();
                    doLeave(p, arena, isSpectator);
                    return;
                }
                String msg = plugin.getMessages().getMsg(p, "BedLeaveConfirmTitle")
                        .replace("/seconds/", String.valueOf(secondsLeft[0]));
                String sub = plugin.getMessages().getMsg(p, "BedLeaveConfirmSubtitle");
                p.sendMessage(msg + " " + sub);
                secondsLeft[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        pendingLeave.put(p.getUniqueId(), task);
    }

    private void doLeave(Player p, IArena arena, boolean isSpectator) {
        arena.leave(p, LeaveReason.COMMAND);
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null) return;
        if (arena.getSpectators().contains(p)) {
            e.setCancelled(true);
        }
    }

    /** Spectators can not be hurt; void damage (no Quick Void Y, or flying down fast) sends them back up. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null) return;
        boolean waiting = arena.isPlayerInArena(p)
                && (arena.status() == GameStatus.waiting || arena.status() == GameStatus.starting);
        if (!arena.getSpectators().contains(p) && !(waiting && e.getCause() == EntityDamageEvent.DamageCause.VOID)) return;
        e.setCancelled(true);
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) rescueFromVoid(p, arena);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        BukkitTask pending = pendingLeave.remove(e.getPlayer().getUniqueId());
        if (pending != null) pending.cancel();
    }
}
