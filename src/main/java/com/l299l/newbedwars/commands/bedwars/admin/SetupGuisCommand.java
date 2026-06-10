package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class SetupGuisCommand extends SubCommand {
    private final Messages msg;

    public SetupGuisCommand() {
        msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "setupGuis";
    }

    @Override
    public String getDescription() {
        return "It allows you to setup and create guis for game shops/upgrades/other and custom items. \n" +
                "You can also create custom guis for custom items or setup lobby guis.";
    }

    @Override
    public String getSyntax() {
        return "/bw setupGuis";
    }

    @Override
    public String getExample() {
        return "/bw setupGuis";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (!player.hasPermission("newbedwars.bw.setupguis") && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "[NewBedwars] " + ChatColor.YELLOW + "Edit GUIs via YAML files in " + ChatColor.WHITE + "plugins/NewBedwars/guis/" + ChatColor.YELLOW + " for now.");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
