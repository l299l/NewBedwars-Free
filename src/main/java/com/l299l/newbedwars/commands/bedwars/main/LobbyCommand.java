package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.utils.LobbyService;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.entity.Player;

import java.util.List;

public class LobbyCommand extends SubCommand {
    private final Messages msg;

    public LobbyCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

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
        IArena playerArena = Arena.arenaByWorld.get(player.getWorld());
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
