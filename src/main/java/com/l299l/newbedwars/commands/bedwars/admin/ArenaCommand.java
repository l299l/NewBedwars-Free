package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.commands.bedwars.admin.managearena.*;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class ArenaCommand extends SubCommand {
    private final Messages msg;
    private final CreateCommand createCommand;
    private final DeleteCommand deleteCommand;
    private final EnableArenaCommand enableArenaCommand;
    private final DisableArenaCommand disableArenaCommand;
    private final SetupArenaCommand setupCommand;
    private final ListArenaCommand listArenaCommand;
    private final List<ArenaMaintenanceCommand> maintenanceCommands;

    public ArenaCommand() {
        msg = NewBedwars.plugin.getMessages();
        createCommand = new CreateCommand();
        deleteCommand = new DeleteCommand();
        enableArenaCommand = new EnableArenaCommand();
        disableArenaCommand = new DisableArenaCommand();
        setupCommand = new SetupArenaCommand();
        listArenaCommand = new ListArenaCommand();
        maintenanceCommands = List.of("backup", "backups", "restore", "export", "import", "copy", "resetmap").stream()
                .map(ArenaMaintenanceCommand::new).toList();
    }

    private ArenaMaintenanceCommand maintenance(String name) {
        return maintenanceCommands.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
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
        return "/bw arena (<create|delete|enable|disable|setup|list|backup|backups|restore|export|import|copy|resetmap>) <arenaName> (options of subcommands)";
    }

    @Override
    public String getExample() {
        return "/bw arena create castle -n";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if(!player.hasPermission("newbedwars.bw.admin") && !player.hasPermission("newbedwars.bw.arena")
                && (args.length < 2 || maintenance(args[1]) == null || !player.hasPermission("newbedwars.bw.arena." + args[1]))) {
            msg.send(player, "NoPermissions");
            return;
        }
        if(args.length > 1) {
            switch (args[1]) {
                case "create" -> createCommand.perform(player, args, old);
                case "delete" -> deleteCommand.perform(player, args, old);
                case "enable" -> enableArenaCommand.perform(player, args, old);
                case "disable" -> disableArenaCommand.perform(player, args, old);
                case "setup" -> setupCommand.perform(player, args, old);
                case "list" -> listArenaCommand.perform(player, args, old);
                default -> {
                    ArenaMaintenanceCommand sub = maintenance(args[1]);
                    if (sub != null) sub.perform(player, args, old);
                    else sendUsage(player);
                }
            }
        } else {
            sendUsage(player);
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(msg.getMsg(player, "ArenaUsageHeader"));
        List<SubCommand> all = new java.util.ArrayList<>(List.of(createCommand, deleteCommand, enableArenaCommand,
                disableArenaCommand, setupCommand, listArenaCommand));
        all.addAll(maintenanceCommands);
        for (SubCommand sub : all) {
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
            return List.of("create", "delete", "enable", "disable", "setup", "list",
                    "backup", "backups", "restore", "export", "import", "copy", "resetmap");
        } else if (args.length >= 3) {
            return switch (args[1]) {
                case "create" -> createCommand.getSubcommandArguments(player, args);
                case "delete" -> deleteCommand.getSubcommandArguments(player, args);
                case "enable" -> enableArenaCommand.getSubcommandArguments(player, args);
                case "disable" -> disableArenaCommand.getSubcommandArguments(player, args);
                case "setup" -> setupCommand.getSubcommandArguments(player, args);
                default -> {
                    ArenaMaintenanceCommand sub = maintenance(args[1]);
                    yield sub != null ? sub.getSubcommandArguments(player, args) : null;
                }
            };
        }
        return null;
    }
}
