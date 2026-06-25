package com.l299l.newbedwars.gui.configuration.game.settings;

import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.configuration.game.guis.FastBuyCustomizerGUI;
import com.l299l.newbedwars.gui.configuration.game.guis.ItemPickerGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ItemPickerGUISettings implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof ItemPickerGUI gui)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();

        if (slot == ItemPickerGUI.BACK_SLOT) {
            player.openInventory(gui.getParent().getInventory());
            return;
        }

        CustomItem ci = gui.getItemAt(slot);
        if (ci == null) return;

        FastBuyCustomizerGUI parent = gui.getParent();
        parent.addItem(gui.getCatIndex(), ci.getName());
        player.openInventory(parent.getInventory());
    }
}
