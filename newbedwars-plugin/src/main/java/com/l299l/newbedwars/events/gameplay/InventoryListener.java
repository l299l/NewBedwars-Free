package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import org.bukkit.entity.*;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import java.util.List;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

/** Item drops, armor slots, shop NPCs, the ender chest, crafting and hunger inside arenas. */
public class InventoryListener implements Listener {

    @EventHandler
    public void playerDropItem(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null) return;
        if (arena.getSpectators().contains(p) ||
                (arena.isPlayerInArena(p) && (arena.status() == GameStatus.waiting || arena.status() == GameStatus.starting))) {
            e.setCancelled(true);
            return;
        }
        if (arena.isPlayerInArena(p) && arena.status() == GameStatus.playing) {
            if (e.getItemDrop().getItemStack().getType().name().endsWith("_SWORD")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void playerInvChange(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        Player p = (Player) e.getWhoClicked();


        if (ArenaRegistry.byWorld(p.getWorld()) != null) {
            IArena arena = ArenaRegistry.byWorld(p.getWorld());
            if (arena.getSpectators().contains(p) ||
                    (arena.isPlayerInArena(p) && (arena.status() == GameStatus.waiting || arena.status() == GameStatus.starting))) {
                e.setCancelled(true);
                return;
            }
            if (arena.isPlayerInArena(p) && arena.status() == GameStatus.playing) {
                // armor is managed by the shop: block taking it off, swapping (incl. number keys) and shift-equipping
                boolean armorSlot = e.getSlotType() == InventoryType.SlotType.ARMOR;
                boolean shiftEquip = e.isShiftClick() && e.getClickedInventory() == p.getInventory()
                        && e.getCurrentItem() != null && isArmorPiece(e.getCurrentItem().getType());
                if (armorSlot || shiftEquip) e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void playerArmorDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || !arena.isPlayerInArena(p) || arena.status() != GameStatus.playing) return;
        for (int slot : e.getRawSlots()) {
            if (e.getView().getSlotType(slot) == InventoryType.SlotType.ARMOR) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private static boolean isArmorPiece(Material m) {
        String n = m.name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS")
                || n.endsWith("_BOOTS") || m == Material.ELYTRA || m == Material.CARVED_PUMPKIN;
    }

    @EventHandler
    public void playerNpcInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (ArenaRegistry.byWorld(p.getWorld()) != null) {
            IArena arena = ArenaRegistry.byWorld(p.getWorld());
            if (arena.isPlayerInArena(p) && (arena.status() == GameStatus.playing)) {
                List<TeamShop> teamShops = arena.getTeamShops();
                List<TeamUpgrades> teamUpgrades = arena.getTeamUpgrades();
                Team team = arena.getTeam(p);
                for (TeamShop shop : teamShops) {
                    if (shop.getNpc() != null && e.getRightClicked().getUniqueId().equals(shop.getNpc().getUniqueId())) {
                        team.getTeamShop().open(p);
                        return;
                    }
                }
                for (TeamUpgrades upgrade : teamUpgrades) {
                    if (upgrade.getNpc() != null && e.getRightClicked().getUniqueId().equals(upgrade.getNpc().getUniqueId())) {
                        team.getTeamUpgrades().open(p);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (ArenaRegistry.byWorld(p.getWorld()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnderChestOpen(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null
                || e.getClickedBlock().getType() != Material.ENDER_CHEST) return;
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        GamePlayer gp = arena.getPlayer(p.getUniqueId());
        if (gp == null || arena.getSpectators().contains(p)) return;
        if (p.isSneaking() && e.getItem() != null) return; // let the player place a block
        e.setCancelled(true);
        p.openInventory(gp.getVirtualEnderChest());
    }

    @EventHandler
    public void onPlayerCraft(CraftItemEvent e) {
        HumanEntity clicker = e.getWhoClicked();
        if (!(clicker instanceof Player)) return;
        Player p = (Player) clicker;

        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;

        e.setCancelled(true);
    }
}
