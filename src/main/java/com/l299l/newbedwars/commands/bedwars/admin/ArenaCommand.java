package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.commands.bedwars.admin.managearena.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArenaCommand extends BwCommand {
    /** Subcommands by name, in the order they are listed in the usage and tab completion. */
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public ArenaCommand() {
        for (SubCommand sub : List.of(new CreateCommand(), new DeleteCommand(), new EnableArenaCommand(),
                new DisableArenaCommand(), new SetupArenaCommand(), new ListArenaCommand())) {
            subCommands.put(sub.getName(), sub);
        }
        for (ArenaMaintenanceCommand.Action action : ArenaMaintenanceCommand.Action.values()) {
            ArenaMaintenanceCommand sub = new ArenaMaintenanceCommand(action);
            subCommands.put(sub.getName(), sub);
        }
    }

    @Override
    public String getName() {
        return "arena";
    }

    @Override
    public String getDescription() {
        return "It allows you to manage arenas. You can create, delete, enable, disable, setup arenas.";
    }

    @Override
    public String getSyntax() {
        return "/bw arena (<" + String.join("|", subCommands.keySet()) + ">) <arenaName> (options of subcommands)";
    }

    @Override
    public String getExample() {
        return "/bw arena create castle -n";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        SubCommand sub = args.length > 1 ? subCommands.get(args[1]) : null;
        if(!player.hasPermission("newbedwars.bw.admin") && !player.hasPermission("newbedwars.bw.arena")
                && !(sub instanceof ArenaMaintenanceCommand && player.hasPermission("newbedwars.bw.arena." + args[1]))) {
            msg.send(player, "NoPermissions");
            return;
        }
        if (sub != null) sub.perform(player, args, old);
        else sendUsage(player);
    }

    private void sendUsage(Player player) {
        player.sendMessage(msg.getMsg(player, "ArenaUsageHeader"));
        for (SubCommand sub : subCommands.values()) {
            String description = msg.getMsg(player, "ArenaUsageDesc-" + sub.getName());
            player.sendMessage(ChatColor.YELLOW + sub.getSyntax() + ChatColor.GRAY + " - "
                    + (description.isEmpty() ? sub.getDescription() : description));
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (!player.hasPermission("newbedwars.bw.admin") && !player.hasPermission("newbedwars.bw.arena")) {
            return null;
        }
        if (args.length == 2) {
            return new ArrayList<>(subCommands.keySet());
        }
        SubCommand sub = args.length >= 3 ? subCommands.get(args[1]) : null;
        return sub != null ? sub.getSubcommandArguments(player, args) : null;
    }
}
