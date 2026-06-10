package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.List;

public class SetLobbyCommand extends SubCommand {
    private final Messages msg;

    public SetLobbyCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "setlobby";
    }

    @Override
    public String getDescription() {
        return "Sets the lobby spawn location players are teleported to after a game.";
    }

    @Override
    public String getSyntax() {
        return "/bw setlobby";
    }

    @Override
    public String getExample() {
        return "/bw setlobby";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (!player.hasPermission("newbedwars.bw.setlobby") && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return;
        }
        NewBedwars.plugin.setLobbyLocation(player.getLocation());
        msg.send(player, "LobbySet");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
