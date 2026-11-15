package com.l299l.newbedwars.gui.configuration.game.listener;

import com.l299l.newbedwars.gui.model.GUIEntry;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.arena.shops.upgrades.UpgradePurchase;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.game.guis.ShopGUI;
import com.l299l.newbedwars.gui.model.GUICategory;
import com.l299l.newbedwars.gui.model.GUIUpgrade;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

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

        if (item instanceof CustomItem it && it.getOnBuyEvent() == OnBuyEvent.UPGRADE) {
            Upgrade upgrade = findUpgrade(it.getName());
            if (upgrade != null) buyUpgrade(player, upgrade, it);
        } else if (item instanceof CustomItem it) {
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

            GUICategory target = cat.id().equalsIgnoreCase("home") ? null : cat;
            ShopGUI newGui = new ShopGUI(NewBedwars.plugin.getGuiManager(), player, gui.getGuiSave(), target);
            player.openInventory(newGui.getInventory());
        } else if (item instanceof GUIUpgrade guiUpgrade) {
            Upgrade upgrade = guiUpgrade.upgrade();
            if (upgrade != null) buyUpgrade(player, upgrade, guiUpgrade.icon());
        }
    }

    private void buyUpgrade(Player player, Upgrade upgrade, CustomItem icon) {
        Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
            IArena arena = ArenaRegistry.byWorld(player.getWorld());
            if (arena == null) return;
            Team team = arena.getTeam(player);
            if (team == null || team.getTeamUpgrades() == null) return;
            UpgradePurchase.Result result = UpgradePurchase.buy(player, arena, team, upgrade, icon.getName());
            if (result.tier() == null || result.outcome() == UpgradePurchase.Outcome.CANCELLED) return;
            Map<String, String> values = Map.of(
                    "/item/", icon.getName(),
                    "/price/", String.valueOf(result.tier().price()),
                    "/priceType/", result.tier().currency().name(),
                    "/amount/", "1");
            if (result.bought()) {
                msg.send(player, "BoughtItem", values);
                team.getTeamUpgrades().open(player);
            } else {
                msg.send(player, "NotEnoughMoney", values);
            }
        });
    }

    private static Upgrade findUpgrade(String name) {
        try {
            return Upgrade.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
