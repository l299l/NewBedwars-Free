package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.player.LobbyService;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.entity.Player;

import java.util.List;

public class LobbyCommand extends BwCommand {


    @Override
    public String getName() {
        return "lobby";
    }

    @Override
    public String getDescription() {
        return "Teleports you to the lobby.";
    }

    @Override
    public String getSyntax() {
        return "/bw lobby";
    }

    @Override
    public String getExample() {
        return "/bw lobby";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (Properties.RequireLobbyPermission && !player.hasPermission("newbedwars.bw.lobby") && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return;
        }
        IArena playerArena = ArenaRegistry.byWorld(player.getWorld());
        if (playerArena != null && (playerArena.isPlayerInArena(player) || playerArena.getSpectators().contains(player))) {
            playerArena.leave(player, LeaveReason.COMMAND);
            return;
        }
        LobbyService.sendToLobby(player, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.LOBBY_COMMAND);
        msg.send(player, "TeleportedToLobby");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
