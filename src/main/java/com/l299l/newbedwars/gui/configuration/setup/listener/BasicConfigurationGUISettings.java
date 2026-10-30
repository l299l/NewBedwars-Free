package com.l299l.newbedwars.gui.configuration.setup.listener;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.guis.BasicConfigurationGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class BasicConfigurationGUISettings implements Listener {
    private final Messages msg;

    public BasicConfigurationGUISettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        try {
            if (e.getClickedInventory().getHolder() instanceof BasicConfigurationGUI) {
                IArena arena = Arena.arenaByWorld.get(player.getWorld());
                if (e.getSlot() == 11) {
                    arena.changeSetup(Setup.NORMAL_SETUP);
                    msg.sendConf(player, Setup.NORMAL_SETUP);
                    player.closeInventory();
                } else if (e.getSlot() == 13) {
                    arena.changeSetup(Setup.ADVANCED_SETUP);
                    msg.sendConf(player, Setup.ADVANCED_SETUP);
                    player.closeInventory();
                } else if (e.getSlot() == 15) {
                    // Automatic setup is not yet implemented
                    player.sendMessage(org.bukkit.ChatColor.RED + "This setup mode is not yet implemented.");
                }
                e.setCancelled(true);
            }
        }catch (Exception exeption) {
            msg.send(player, "ArenaNotExists");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        if (e.getInventory().getHolder() instanceof BasicConfigurationGUI) {
            IArena arena = Arena.arenaByWorld.get(player.getWorld());
            if (arena.getSetupMode().equals(Setup.NO_SETUP)) {
                player.sendMessage(ChatColor.RED + "You have to choose setup mode!");
                Bukkit.getScheduler().runTaskLater(NewBedwars.plugin, () -> {
                    player.openInventory(e.getInventory());
                }, 1L);
            }
        }
    }
}
