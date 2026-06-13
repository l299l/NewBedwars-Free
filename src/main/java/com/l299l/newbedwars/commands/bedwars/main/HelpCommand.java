package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HelpCommand extends SubCommand {
    private final ArrayList<SubCommand> lobbyCommands;
    private final ArrayList<SubCommand> gameAdminCommands;

    public HelpCommand(ArrayList<SubCommand> lobbyCommands, ArrayList<SubCommand> gameAdminCommands) {
        this.lobbyCommands = lobbyCommands;
        this.gameAdminCommands = gameAdminCommands;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows available commands.";
    }

    @Override
    public String getSyntax() {
        return "/bw help";
    }

    @Override
    public String getExample() {
        return "/bw help";
    }

    private static final Set<String> USER_COMMANDS = Set.of("join", "rejoin", "lobby", "lang", "spectate");

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        boolean isAdmin = player.hasPermission("newbedwars.bw.admin") || player.isOp();
        player.sendMessage(ChatColor.AQUA + "=-=-=-=-=-= NewBedwars Help =-=-=-=-=-=");
        if (isAdmin) {
            for (SubCommand cmd : lobbyCommands) {
                if (cmd.getName().equalsIgnoreCase("help")) continue;
                player.sendMessage(ChatColor.YELLOW + cmd.getSyntax() + ChatColor.GRAY + " - " + cmd.getDescription());
            }
            player.sendMessage(ChatColor.RED + "-- In-game admin --");
            for (SubCommand cmd : gameAdminCommands) {
                player.sendMessage(ChatColor.YELLOW + cmd.getSyntax() + ChatColor.GRAY + " - " + cmd.getDescription());
            }
        } else {
            for (SubCommand cmd : lobbyCommands) {
                if (USER_COMMANDS.contains(cmd.getName())) {
                    player.sendMessage(ChatColor.YELLOW + cmd.getSyntax() + ChatColor.GRAY + " - " + cmd.getDescription());
                }
            }
        }
        player.sendMessage(ChatColor.DARK_AQUA + "Party: " + ChatColor.YELLOW + "/party help " + ChatColor.GRAY + "- Manage your party (/p is an alias)");
        player.sendMessage(ChatColor.AQUA + "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
