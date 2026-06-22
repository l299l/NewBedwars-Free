package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.commands.bedwars.admin.manageArena.*;
import com.l299l.newbedwars.config.Messages;
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

    public ArenaCommand() {
        msg = NewBedwars.plugin.getMessages();
        createCommand = new CreateCommand();
        deleteCommand = new DeleteCommand();
        enableArenaCommand = new EnableArenaCommand();
        disableArenaCommand = new DisableArenaCommand();
        setupCommand = new SetupArenaCommand();
        listArenaCommand = new ListArenaCommand();
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
        return "/bw arena (<create|delete|enable|disable|setup|list>) <arenaName> (options of subcommands)";
    }

    @Override
    public String getExample() {
        return "/bw arena create castle -n";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if(!player.hasPermission("newbedwars.bw.admin") && !player.hasPermission("newbedwars.bw.arena")) {
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
                default -> player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            }
        } else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (!player.hasPermission("newbedwars.bw.admin") && !player.hasPermission("newbedwars.bw.arena")) {
            return null;
        }
        if (args.length == 2) {
            return List.of("create", "delete", "enable", "disable", "setup", "list");
        } else if (args.length >= 3) {
            return switch (args[1]) {
                case "create" -> createCommand.getSubcommandArguments(player, args);
                case "delete" -> deleteCommand.getSubcommandArguments(player, args);
                case "enable" -> enableArenaCommand.getSubcommandArguments(player, args);
                case "disable" -> disableArenaCommand.getSubcommandArguments(player, args);
                case "setup" -> setupCommand.getSubcommandArguments(player, args);
                default -> null;
            };
        }
        return null;
    }
}
