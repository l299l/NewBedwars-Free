package com.l299l.newbedwars.gui.configuration.setup.listener;

import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.configuration.setup.guis.ArenaInfoGUI;
import com.l299l.newbedwars.gui.configuration.setup.guis.ArenaInfoTeamGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ArenaInfoGUISettings implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        try {
            if (e.getClickedInventory().getHolder() instanceof ArenaInfoGUI gui) {
                e.setCancelled(true);
                Team team = gui.getTeam(e.getSlot());
                if (team != null) {
                    player.openInventory(new ArenaInfoTeamGUI(player, gui.getArena(), team).getInventory());
                } else if (e.getSlot() == 34 && gui.getPage() > 0) {
                    player.openInventory(new ArenaInfoGUI(player, gui.getArena(), gui.getPage() - 1).getInventory());
                } else if (e.getSlot() == 35 && gui.getPage() < gui.getTotalPages() - 1) {
                    player.openInventory(new ArenaInfoGUI(player, gui.getArena(), gui.getPage() + 1).getInventory());
                }
            } else if (e.getClickedInventory().getHolder() instanceof ArenaInfoTeamGUI gui) {
                e.setCancelled(true);
                player.openInventory(new ArenaInfoGUI(player, gui.getArena()).getInventory());
            }
        } catch (Exception exception) {
            e.setCancelled(true);
        }
    }
}
