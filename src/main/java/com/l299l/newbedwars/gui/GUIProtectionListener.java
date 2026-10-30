package com.l299l.newbedwars.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Stops items from being moved into (or pulled out of) any plugin GUI from the player's own
 * inventory: shift-clicks, double-click collecting and drags. The per-GUI listeners only look at
 * clicks inside their own inventory.
 */
public class GUIProtectionListener implements Listener {
    private static boolean isPluginGui(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder != null && holder.getClass().getName().startsWith("com.l299l.newbedwars.gui.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!isPluginGui(e.getView().getTopInventory())) return;
        InventoryAction action = e.getAction();
        if (e.isShiftClick()
                || e.getClick() == ClickType.DOUBLE_CLICK
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (!isPluginGui(e.getView().getTopInventory())) return;
        int topSize = e.getView().getTopInventory().getSize();
        for (int slot : e.getRawSlots()) {
            if (slot < topSize) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
