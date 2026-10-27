package com.l299l.newbedwars.commands;

import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.utils.LobbyService;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LobbyStandaloneCommand implements CommandExecutor {
    private final Messages msg;

    public LobbyStandaloneCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NewBedwars.plugin.getMessages().getMsgToConsole("OnlyPlayer"));
            return true;
        }
        if (Properties.RequireLobbyPermission && !player.hasPermission("newbedwars.bw.lobby") && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return true;
        }
        IArena playerArena = Arena.arenaByWorld.get(player.getWorld());
        if (playerArena != null && (playerArena.isPlayerInArena(player) || playerArena.getSpectators().contains(player))) {
            playerArena.leave(player, LeaveReason.COMMAND);
            return true;
        }
        LobbyService.sendToLobby(player, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.LOBBY_COMMAND);
        msg.send(player, "TeleportedToLobby");
        return true;
    }
}
