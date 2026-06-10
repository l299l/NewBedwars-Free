package com.l299l.newbedwars.gui.configuration.setup.settings;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.guis.ConfirmLeaveGUI;
import com.l299l.newbedwars.utils.DecoUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Collection;

public class ConfirmLeaveGuiSettings implements Listener {

    private final Messages msg;

    public ConfirmLeaveGuiSettings() {
        msg = NewBedwars.plugin.getMessages();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        try {
            if (e.getClickedInventory().getHolder() instanceof ConfirmLeaveGUI) {
                IArena arena = Arena.arenaByWorld.get(player.getWorld());
                if(!((ConfirmLeaveGUI) e.getClickedInventory().getHolder()).getAll()) {
                    if (e.getSlot() > 0 && e.getSlot() < 4) {
                        leave(arena, player);
                        msg.send(player, "SuccessfullyLeave");
                    } else {
                        if (e.getSlot() != 4) {
                            e.setCancelled(true);
                            player.closeInventory();
                        }
                        e.setCancelled(true);
                    }
                    e.setCancelled(true);
                }else {
                    if (e.getSlot() > 0 && e.getSlot() < 4) {
                        Collection<? extends Player> cAllPlayers = Bukkit.getServer().getOnlinePlayers();
                        ArrayList<Player> allPlayers = new ArrayList<>(cAllPlayers);
                        for (Player allPlayer : allPlayers) {
                            if (arena.getArenaWorld() == allPlayer.getWorld()) {
                                if (arena.getPlayersOnSetup().contains(allPlayer.getUniqueId())) {
                                    if (!allPlayer.hasPermission("newbedwars.bw.bypass") || allPlayer == player) {
                                        leave(arena, allPlayer);
                                        msg.send(allPlayer, "KickedByOtherAdmin");
                                    } else {
                                        player.sendMessage(msg.getMsg(player, "AdminHaveBypass").replaceAll("/admin/", allPlayer.getName()));
                                    }
                                }
                            }
                        }
                    }else {
                        if (e.getSlot() != 4) {
                            e.setCancelled(true);
                            player.closeInventory();
                        }
                        e.setCancelled(true);
                    }
                    e.setCancelled(true);
                }

            }
        }catch (Exception exeption) {
            msg.send(player, "ArenaNotExists");
            e.setCancelled(true);
        }
    }

    private void leave(IArena arena, Player player) {
        arena.getPlayersOnSetup().remove(player.getUniqueId());
        if (arena.getPlayersOnSetup().isEmpty()) {
            DecoUtils.removeAllArmorStands(arena);
        }
        player.teleport(NewBedwars.plugin.getLobbyLocation());
        player.closeInventory();
        player.setGameMode(GameMode.SURVIVAL);
    }
}
