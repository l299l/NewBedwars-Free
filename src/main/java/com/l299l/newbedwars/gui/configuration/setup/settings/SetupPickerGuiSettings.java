package com.l299l.newbedwars.gui.configuration.setup.settings;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.SetupGUI;
import com.l299l.newbedwars.gui.configuration.setup.guis.SetupPickerGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SetupPickerGuiSettings implements Listener {
    private final Messages msg;

    public SetupPickerGuiSettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        if (e.getClickedInventory().getHolder() instanceof SetupPickerGUI gui) {
            if (e.getSlot() > gui.getSetups().size() - 1) {
                e.setCancelled(true);
                return;
            }
            SetupGUI setupGUI = gui.getSetups().get(e.getSlot());
            player.closeInventory();
            player.openInventory(setupGUI.getInventory());
            e.setCancelled(true);
        }
    }
}
