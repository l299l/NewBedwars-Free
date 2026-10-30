package com.l299l.newbedwars.gui.configuration.game.settings;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ArenaSelectGUISettings implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        if (!(e.getInventory().getHolder() instanceof ArenaSelectGUI gui)) {
            return;
        }
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) {
            return;
        }
        IArena target = gui.getArena(e.getRawSlot());
        if (target == null) {
            return;
        }
        p.closeInventory();
        // Same checks as /bw join <arena>: permission, party admin, rejoin slot in another arena, state, room.
        ArenaJoinService.join(p, target);
    }
}
