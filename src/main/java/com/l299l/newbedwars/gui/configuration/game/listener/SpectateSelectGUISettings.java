package com.l299l.newbedwars.gui.configuration.game.listener;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.game.guis.SpectateSelectGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SpectateSelectGUISettings implements Listener {
    private final Messages msg;

    public SpectateSelectGUISettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        if (!(e.getInventory().getHolder() instanceof SpectateSelectGUI gui)) {
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

        if (target.status() != GameStatus.playing && target.status() != GameStatus.ending) {
            msg.send(p, "ArenaNotRunningForSpectate");
            return;
        }
        if (target.getGamerules() == null || !target.getGamerules().AllowSpectators) {
            msg.send(p, "SpectatingDisabled");
            return;
        }
        if (!target.joinAsSpectator(p)) {
            msg.send(p, "SpectateFailed");
        }
    }
}
