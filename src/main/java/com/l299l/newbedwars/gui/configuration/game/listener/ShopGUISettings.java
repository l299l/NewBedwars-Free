package com.l299l.newbedwars.gui.configuration.game.listener;

import com.l299l.newbedwars.gui.model.GUIEntry;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.api.events.ShopPurchaseEvent;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.configuration.game.guis.ShopGUI;
import com.l299l.newbedwars.gui.model.GUICategory;
import com.l299l.newbedwars.gui.model.GUIUpgrade;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ShopGUISettings implements Listener {
    private final Messages msg;

    public ShopGUISettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        if (!(e.getClickedInventory().getHolder() instanceof ShopGUI gui)) {
            return;
        }
        e.setCancelled(true);

        int slot = e.getSlot();
        GUIEntry item = gui.getItemOnSlot(slot);
        if (item == null) return;

        if (item instanceof CustomItem it) {
            Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
                if (it.buyItem(player)) {
                    msg.send(player, "BoughtItem", Map.of(
                            "/item/", it.getName(),
                            "/price/", String.valueOf(it.getPrice()),
                            "/priceType/", it.getPriceType().toString(),
                            "/amount/", String.valueOf(it.getAmount())));
                } else {
                    msg.send(player, "NotEnoughMoney", Map.of(
                            "/item/", it.getName(),
                            "/price/", String.valueOf(it.getPrice()),
                            "/priceType/", it.getPriceType().toString(),
                            "/amount/", String.valueOf(it.getAmount())));
                }
            });
        } else if (item instanceof GUICategory cat) {
            player.closeInventory();
            // "home" category → navigate back to the home page (null category)
            GUICategory target = cat.id().equalsIgnoreCase("home") ? null : cat;
            ShopGUI newGui = new ShopGUI(NewBedwars.plugin.getGuiManager(), player, gui.getGuiSave(), target);
            player.openInventory(newGui.getInventory());
        } else if (item instanceof GUIUpgrade guiUpgrade) {
            Arena arena = (Arena) ArenaRegistry.byWorld(player.getWorld());
            if (arena == null) return;
            Team team = arena.getTeam(player);
            if (team == null) return;
            TeamUpgrades tu = team.getTeamUpgrades();
            if (tu == null) return;
            Upgrade upgrade = guiUpgrade.upgrade();
            if (upgrade == null) return;
            int currentLevel = tu.getUpgradeLevel(upgrade);
            if (currentLevel >= upgrade.maxLevel) return;
            int price = Properties.getUpgradePrice(upgrade, currentLevel);
            PriceType priceType = Properties.UpgradePriceType;
            Material mat = Properties.getMaterialForPriceType(priceType);
            CustomItem icon = guiUpgrade.icon();
            if (!player.getInventory().contains(mat, price)) {
                msg.send(player, "NotEnoughMoney", Map.of(
                        "/item/", icon.getName(),
                        "/price/", String.valueOf(price),
                        "/priceType/", priceType.name(),
                        "/amount/", "1"));
                return;
            }
            if (!ApiEvents.shopPurchase(arena, player, icon.getName(), ShopPurchaseEvent.Kind.UPGRADE, priceType.name(), price)
                    || !ApiEvents.teamUpgrade(arena, team, upgrade.name(), currentLevel + 1, player)) return;
            Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
                // a second click in the same tick must not buy the same level twice
                if (tu.getUpgradeLevel(upgrade) != currentLevel || !player.getInventory().contains(mat, price)) return;
                player.getInventory().removeItem(new ItemStack(mat, price));
                tu.upgrade(upgrade);
                msg.send(player, "BoughtItem", Map.of(
                        "/item/", icon.getName(),
                        "/price/", String.valueOf(price),
                        "/priceType/", priceType.name(),
                        "/amount/", "1"));
                tu.open(player);
            });
        }
    }
}
