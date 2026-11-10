package com.l299l.newbedwars.gui.configuration.setup.listener;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.guis.DeleteConfirmGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class DeleteConfirmGUISettings implements Listener {
    private final Messages msg;

    public DeleteConfirmGUISettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (!(e.getClickedInventory().getHolder() instanceof DeleteConfirmGUI gui)) return;

        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        if (slot >= 0 && slot < 4) {
            player.closeInventory();
            deleteArena(gui.getArenaName(), player);
        } else if (slot >= 5 && slot < 9) {
            player.closeInventory();
        }
    }

    private void deleteArena(String arenaName, Player player) {
        ArenaMaintenanceService.delete(arenaName)
                .thenAccept(outcome -> msg.send(player, outcome.key(), outcome.replacements()));
    }
}
