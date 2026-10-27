package com.l299l.newbedwars.utils;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** The single way the plugin sends a player to the lobby; addons can cancel or redirect it. */
public final class LobbyService {
    private LobbyService() {}

    /** @return true when the player was teleported by the plugin, false when a listener took over. */
    public static boolean sendToLobby(Player player, PlayerSendToLobbyEvent.Reason reason) {
        PlayerSendToLobbyEvent event = new PlayerSendToLobbyEvent(player, reason, NewBedwars.plugin.getLobbyLocation());
        Bukkit.getPluginManager().callEvent(event);
        Location target = event.getTarget();
        if (event.isCancelled() || target == null) return false;
        player.teleport(target);
        return true;
    }
}
