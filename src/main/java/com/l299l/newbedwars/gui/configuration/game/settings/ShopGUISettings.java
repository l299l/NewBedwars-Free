package com.l299l.newbedwars.gui.configuration.game.settings;

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
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiCategory;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiUpgrade;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

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
        Object item = gui.getItemOnSlot(slot);
        if (item == null) return;

        if (item instanceof CustomItem it) {
            Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
                if (it.buyItem(player)) {
                    msg.send(player, "BoughtItem", new HashMap<>() {{
                        put("/item/", it.getName());
                        put("/price/", String.valueOf(it.getPrice()));
                        put("/priceType/", it.getPriceType().toString());
                        put("/amount/", String.valueOf(it.getAmount()));
                    }});
                } else {
                    msg.send(player, "NotEnoughMoney", new HashMap<>() {{
                        put("/item/", it.getName());
                        put("/price/", String.valueOf(it.getPrice()));
                        put("/priceType/", it.getPriceType().toString());
                        put("/amount/", String.valueOf(it.getAmount()));
                    }});
                }
            });
        } else if (item instanceof GuiCategory cat) {
            player.closeInventory();
            // "home" category → navigate back to the home page (null category)
            GuiCategory target = cat.id().equalsIgnoreCase("home") ? null : cat;
            ShopGUI newGui = new ShopGUI(NewBedwars.plugin.getGuiManager(), player, gui.getGuiSave(), target);
            player.openInventory(newGui.getInventory());
        } else if (item instanceof GuiUpgrade guiUpgrade) {
            Arena arena = (Arena) Arena.arenaByWorld.get(player.getWorld());
            if (arena == null) return;
            Team team = arena.getTeam(player);
            if (team == null) return;
            TeamUpgrades tu = team.getTeamUpgrades();
            if (tu == null) return;
            Upgrade upgrade = guiUpgrade.upgrade();
            int currentLevel = tu.getUpgradeLevel(upgrade);
            if (currentLevel >= upgrade.maxLevel) return;
            int price = Properties.getUpgradePrice(upgrade, currentLevel);
            PriceType priceType = Properties.UpgradePriceType;
            Material mat = Properties.getMaterialForPriceType(priceType);
            CustomItem icon = guiUpgrade.icon();
            if (player.getInventory().contains(mat, price)) {
                Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
                    player.getInventory().removeItem(new ItemStack(mat, price));
                    tu.upgrade(upgrade);
                    msg.send(player, "BoughtItem", new HashMap<>() {{
                        put("/item/", icon.getName());
                        put("/price/", String.valueOf(price));
                        put("/priceType/", priceType.name());
                        put("/amount/", "1");
                    }});
                    tu.open(player);
                });
            } else {
                msg.send(player, "NotEnoughMoney", new HashMap<>() {{
                    put("/item/", icon.getName());
                    put("/price/", String.valueOf(price));
                    put("/priceType/", priceType.name());
                    put("/amount/", "1");
                }});
            }
        }
    }
}
