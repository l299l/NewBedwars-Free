package com.l299l.newbedwars.gui.configuration.game.listener;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUISave;
import com.l299l.newbedwars.gui.configuration.game.guis.FastBuyCustomizerGUI;
import com.l299l.newbedwars.gui.configuration.game.guis.ProfileGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ProfileGUISettings implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof ProfileGUI gui)) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;

        int slot = e.getRawSlot();
        if (slot == 26) {
            player.closeInventory();
        } else if (slot == 22 && e.getCurrentItem().getType() == Material.CHEST) {
            if (player.equals(gui.getTarget())) {
                IArena arena = ArenaRegistry.byWorld(player.getWorld());
                String shopGuiId = arena != null ? arena.getShopGuiId() : Properties.DefaultTeamShopGui;
                GUISave shopSave = shopGuiId != null
                        ? NewBedwars.plugin.getGuiManager().getGui(shopGuiId) : null;
                if (shopSave != null) {
                    player.openInventory(new FastBuyCustomizerGUI(
                            NewBedwars.plugin.getGuiManager(), player, shopSave).getInventory());
                }
            }
        }
    }
}
