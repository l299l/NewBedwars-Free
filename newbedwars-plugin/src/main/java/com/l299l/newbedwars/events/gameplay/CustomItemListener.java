package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.RegisteredLogic;
import com.l299l.newbedwars.utils.TeamColor;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.BridgeEggLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.NoneLogic;
import org.bukkit.entity.*;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import java.util.HashSet;
import java.util.UUID;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

/** Using shop custom items: bridge eggs, fireballs, potions, golems, silverfish, towers and addon logic. */
public class CustomItemListener implements Listener {
    private final NewBedwars plugin;
    private final HashSet<UUID> pendingBridgeEggThrow = new HashSet<>();
    private final HashSet<UUID> potionHandledByInteract = new HashSet<>();

    public CustomItemListener(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCustomItemInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) return;

        ItemStack item = e.getItem() != null ? e.getItem() : p.getInventory().getItemInMainHand();
        CustomItem customItem = findCustomItem(item);
        if (customItem == null || customItem.getEvent() instanceof NoneLogic) return;

        LogicType logicType = customItem.getEvent().getType();
        switch (logicType) {
            case BRIDGE_EGG -> {
                pendingBridgeEggThrow.add(p.getUniqueId());
            }
            case FIREBALL -> {
                e.setCancelled(true);
                customItem.getEvent().perform(p, arena);
                consumeOneItem(p);
            }
            case POTION -> {
                e.setCancelled(true);
                consumeOneItem(p);
                final String potionName = customItem.getName();
                potionHandledByInteract.add(p.getUniqueId());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    potionHandledByInteract.remove(p.getUniqueId());
                    PotionEffect effect = getPotionEffect(potionName);
                    if (effect != null) p.addPotionEffect(effect);
                });
            }
            case IRON_GOLEM, SILVERFISH -> {
                e.setCancelled(true);
                boolean success = customItem.getEvent().perform(p, arena);
                if (success) {
                    consumeOneItem(p);
                }
            }
            case PORTABLE_TOWER -> {
                e.setCancelled(true);
                boolean success = customItem.getEvent().perform(p, arena);
                if (success) {
                    // PortableTowerLogic manually consumes the item, so don't consume again
                }
            }
            default -> {
                // logic registered by an addon
                if (customItem.getEvent() instanceof RegisteredLogic) {
                    e.setCancelled(true);
                    if (customItem.getEvent().perform(p, arena)) consumeOneItem(p);
                }
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Egg egg)) return;
        if (!(egg.getShooter() instanceof Player p)) return;
        if (!pendingBridgeEggThrow.remove(p.getUniqueId())) return;

        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        Team team = arena.getTeam(p);
        if (team == null) return;

        Material woolType = TeamColor.wool(team.getColor());
        BridgeEggLogic.startTracking(egg.getUniqueId(), woolType, arena, egg);
    }

    @EventHandler
    public void onBridgeEggThrow(PlayerEggThrowEvent e) {
        if (BridgeEggLogic.pendingBridgeEggs.containsKey(e.getEgg().getUniqueId())) {
            e.setHatching(false);
        }
    }

    @EventHandler
    public void onCustomPotionConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p)) return;
        CustomItem customItem = findCustomItem(e.getItem());
        if (customItem != null && customItem.getEvent().getType() == LogicType.POTION) {
            e.setCancelled(true);
            if (!potionHandledByInteract.remove(p.getUniqueId())) {
                consumeOneItem(p);
                final String potionName = customItem.getName();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    PotionEffect effect = getPotionEffect(potionName);
                    if (effect != null) p.addPotionEffect(effect);
                });
            }
            return;
        }
        if (e.getItem().getType() == Material.POTION) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand != null && inHand.getType() == Material.GLASS_BOTTLE) {
                    p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    p.updateInventory();
                }
            }, 1L);
        }
    }

    private PotionEffect getPotionEffect(String itemName) {
        return switch (itemName.toLowerCase()) {
            case "speed"        -> VersionCompat.SPEED        != null ? new PotionEffect(VersionCompat.SPEED,        200, 1, true, true) : null;
            case "jump"         -> VersionCompat.JUMP_BOOST   != null ? new PotionEffect(VersionCompat.JUMP_BOOST,   200, 1, true, true) : null;
            case "inviscibility"-> VersionCompat.INVISIBILITY != null ? new PotionEffect(VersionCompat.INVISIBILITY, 600, 0, true, true) : null;
            default -> null;
        };
    }

    private void consumeOneItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pendingBridgeEggThrow.remove(e.getPlayer().getUniqueId());
    }
}
