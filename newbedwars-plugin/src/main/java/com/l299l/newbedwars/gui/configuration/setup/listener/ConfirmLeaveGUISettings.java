package com.l299l.newbedwars.gui.configuration.setup.listener;

import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.player.LobbyService;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.guis.ConfirmLeaveGUI;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;

public class ConfirmLeaveGUISettings implements Listener {

    private final Messages msg;

    public ConfirmLeaveGUISettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        if (!(e.getClickedInventory().getHolder() instanceof ConfirmLeaveGUI gui)) return;
        e.setCancelled(true);
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        if (arena == null) {
            msg.send(player, "ArenaNotExists");
            return;
        }
        boolean confirm = e.getSlot() >= 0 && e.getSlot() < 4;
        if (!confirm) {
            if (e.getSlot() != 4) player.closeInventory();
        } else if (!gui.getAll()) {
            leave(arena, player);
            msg.send(player, "SuccessfullyLeave");
        } else {
            for (Player allPlayer : new ArrayList<>(Bukkit.getServer().getOnlinePlayers())) {
                if (arena.getArenaWorld() != allPlayer.getWorld() || !arena.getPlayersOnSetup().contains(allPlayer.getUniqueId())) continue;
                if (!allPlayer.hasPermission("newbedwars.bw.bypass") || allPlayer == player) {
                    leave(arena, allPlayer);
                    msg.send(allPlayer, "KickedByOtherAdmin");
                } else {
                    player.sendMessage(msg.getMsg(player, "AdminHaveBypass").replace("/admin/", allPlayer.getName()));
                }
            }
        }
    }

    private void leave(IArena arena, Player player) {
        arena.getPlayersOnSetup().remove(player.getUniqueId());
        if (arena.getPlayersOnSetup().isEmpty()) {
            DecoUtils.removeAllArmorStands(arena);
        }
        LobbyService.sendToLobby(player, PlayerSendToLobbyEvent.Reason.SETUP_ENDED);
        player.closeInventory();
        player.setGameMode(GameMode.SURVIVAL);
    }
}
