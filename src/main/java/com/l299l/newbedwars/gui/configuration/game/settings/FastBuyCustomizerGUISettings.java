package com.l299l.newbedwars.gui.configuration.game.settings;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.gui.configuration.game.guis.FastBuyCustomizerGUI;
import com.l299l.newbedwars.gui.configuration.game.guis.ItemPickerGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FastBuyCustomizerGUISettings implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof FastBuyCustomizerGUI gui)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();

        // Save & Close (slot 53)
        if (slot == FastBuyCustomizerGUI.saveSlot()) {
            NewBedwars.plugin.getPlayerManager().updateFastBuy(player.getName(), gui.getSelection());
            player.closeInventory();
            NewBedwars.plugin.getMessages().send(player, "FastBuySaved");
            return;
        }

        // Reset All (slot 49)
        if (slot == FastBuyCustomizerGUI.resetSlot()) {
            gui.resetAll();
            NewBedwars.plugin.getMessages().send(player, "FastBuyReset");
            return;
        }

        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;

        // Category icon click → open item picker
        int iconCatIdx = gui.catIndexForIconSlot(slot);
        if (iconCatIdx >= 0) {
            player.openInventory(new ItemPickerGUI(
                    NewBedwars.plugin.getGuiManager(), player, gui, iconCatIdx).getInventory());
            return;
        }

        // Fast-buy slot: filled → remove; empty → open picker
        int fbCatIdx = gui.catIndexForSlot(slot);
        if (fbCatIdx >= 0) {
            int fbIdx = gui.fbIndexForSlot(fbCatIdx, slot);
            if (gui.isSlotFilled(fbCatIdx, fbIdx)) {
                gui.removeFbSlot(fbCatIdx, fbIdx);
            } else {
                player.openInventory(new ItemPickerGUI(
                        NewBedwars.plugin.getGuiManager(), player, gui, fbCatIdx).getInventory());
            }
        }
    }
}
