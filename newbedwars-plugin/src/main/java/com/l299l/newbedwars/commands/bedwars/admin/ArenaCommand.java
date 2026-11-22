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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class ArenaCommand extends BwCommand {

    private static final List<String> ACTIONS = Stream.concat(
            Stream.of("create", "delete", "enable", "disable", "setup", "list"),
            Stream.of(ArenaMaintenanceCommand.Action.values()).map(a -> a.name().toLowerCase(Locale.ROOT))).toList();

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

    public static boolean hasParentPermission(Player player) {
        return player.hasPermission("newbedwars.bw.admin") || player.hasPermission("newbedwars.bw.arena");
    }

    public static boolean canUse(Player player, String action) {
        return hasParentPermission(player) || player.hasPermission("newbedwars.bw.arena." + action);
    }

    public static boolean hasAnyPermission(Player player) {
        if (hasParentPermission(player)) return true;
        for (String action : ACTIONS) {
            if (player.hasPermission("newbedwars.bw.arena." + action)) return true;
        }
        return false;
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : null;
        if (com.l299l.newbedwars.network.NetworkManager.isLobby()) {
            boolean allowed = "list".equals(action) ? canUse(player, "list") : hasParentPermission(player);
            if (!allowed) {
                msg.send(player, "NoPermissions");
                return;
            }
            com.l299l.newbedwars.network.NetworkArenaCommands.perform(player, action);
            return;
        }
        if (!hasAnyPermission(player)) {
            msg.send(player, "NoPermissions");
            return;
        }
        SubCommand sub = action != null ? subCommands.get(action) : null;
        if (sub == null) {
            sendUsage(player);
            return;
        }
        if (!canUse(player, action)) {
            msg.send(player, "NoPermissions");
            return;
        }
        sub.perform(player, args, old);
    }

    private void sendUsage(Player player) {
        player.sendMessage(msg.getMsg(player, "ArenaUsageHeader"));
        for (SubCommand sub : subCommands.values()) {
            if (!canUse(player, sub.getName())) continue;
            String description = msg.getMsg(player, "ArenaUsageDesc-" + sub.getName());
            player.sendMessage(ChatColor.YELLOW + sub.getSyntax() + ChatColor.GRAY + " - "
                    + (description.isEmpty() ? sub.getDescription() : description));
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (!hasAnyPermission(player)) {
            return null;
        }
        if (com.l299l.newbedwars.network.NetworkManager.isLobby()) {
            if (args.length != 2) return null;
            List<String> allowed = new ArrayList<>();
            for (String name : com.l299l.newbedwars.network.NetworkArenaCommands.ALLOWED) {
                if (name.equals("list") ? canUse(player, "list") : hasParentPermission(player)) allowed.add(name);
            }
            return allowed;
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (String name : subCommands.keySet()) {
                if (canUse(player, name)) names.add(name);
            }
            return names;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        SubCommand sub = args.length >= 3 && canUse(player, action) ? subCommands.get(action) : null;
        return sub != null ? sub.getSubcommandArguments(player, args) : null;
    }
}
