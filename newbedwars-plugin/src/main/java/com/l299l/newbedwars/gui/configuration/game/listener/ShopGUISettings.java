package com.l299l.newbedwars.gui.configuration.game.listener;

import com.l299l.newbedwars.gui.model.GUIEntry;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.PurchaseResult;
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
            if (upgrade != null) buyUpgrade(player, upgrade, it, gui);
        } else if (item instanceof CustomItem it) {
            Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
                com.l299l.newbedwars.arena.shops.customitems.ItemPricing.Offer offer = gui.offer(it);
                Map<String, String> values = Map.of(
                        "/item/", it.getName(),
                        "/price/", String.valueOf(offer.price()),
                        "/priceType/", offer.priceLabel(),
                        "/amount/", String.valueOf(offer.amount()));
                PurchaseResult result = it.buyItem(player, offer);
                if (result.bought()) {
                    boolean freeMenuLink = it.getOnBuyEvent() == OnBuyEvent.OPEN_GUI && offer.price() <= 0;
                    if (!freeMenuLink) msg.send(player, "BoughtItem", values);
                } else if (result.messageKey() != null) {
                    msg.send(player, result.messageKey(), values);
                }
            });
        } else if (item instanceof GUICategory cat) {
            GUICategory target = cat.id().equalsIgnoreCase("home") ? null : cat;
            ShopGUI newGui = new ShopGUI(NewBedwars.plugin.getGuiManager(), player, gui.getGuiSave(), target);
            player.openInventory(newGui.getInventory());
        } else if (item instanceof GUIUpgrade guiUpgrade) {
            Upgrade upgrade = guiUpgrade.upgrade();
            if (upgrade != null) buyUpgrade(player, upgrade, guiUpgrade.icon(), gui);
        }
    }

    private void buyUpgrade(Player player, Upgrade upgrade, CustomItem icon, ShopGUI gui) {
        Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
            IArena arena = ArenaRegistry.byWorld(player.getWorld());
            if (!UpgradePurchase.canBuy(arena, player)) {
                msg.send(player, PurchaseResult.UNAVAILABLE.messageKey());
                return;
            }
            Team team = arena.getTeam(player);
            if (team == null || team.getTeamUpgrades() == null) return;
            UpgradePurchase.Result result = UpgradePurchase.buy(player, arena, team, upgrade, icon.getName());
            if (result.outcome() == UpgradePurchase.Outcome.MAXED) {
                msg.send(player, PurchaseResult.MAXED.messageKey(), Map.of("/item/", icon.getName()));
                return;
            }
            if (result.outcome() == UpgradePurchase.Outcome.UNAVAILABLE) {
                msg.send(player, PurchaseResult.UNAVAILABLE.messageKey());
                return;
            }
            if (result.tier() == null || result.outcome() == UpgradePurchase.Outcome.CANCELLED) return;
            Map<String, String> values = Map.of(
                    "/item/", icon.getName(),
                    "/price/", String.valueOf(result.tier().price()),
                    "/priceType/", result.tier().currency().name(),
                    "/amount/", "1");
            if (result.bought()) {
                msg.send(player, "BoughtItem", values);
                if (player.getOpenInventory().getTopInventory().getHolder() == gui) gui.refresh();
                else team.getTeamUpgrades().open(player);
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
