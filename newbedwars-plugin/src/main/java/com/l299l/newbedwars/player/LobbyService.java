package com.l299l.newbedwars.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class LobbyService {
    private LobbyService() {}

    public static boolean sendToLobby(Player player, PlayerSendToLobbyEvent.Reason reason) {
        PlayerSendToLobbyEvent event = new PlayerSendToLobbyEvent(player, reason, NewBedwars.plugin.getLobbyLocation());
        Bukkit.getPluginManager().callEvent(event);
        Location target = event.getTarget();
        if (event.isCancelled() || target == null) return false;
        if (player.teleport(target)) return true;
        NewBedwars.plugin.getLogger().warning("Could not teleport " + player.getName() + " to the lobby (" + reason + ")");
        return false;
    }
}
