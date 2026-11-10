package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class DisableArenaCommand extends BwCommand {

    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public String getDescription() {
        return "Command to disable arena!";
    }

    @Override
    public String getSyntax() {
        return "/bw arena disable <arenaName>";
    }

    @Override
    public String getExample() {
        return "/bw arena disable castle";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (args.length != 3) {
            usage(player);
            return;
        }
        ArenaMaintenanceService.disable(args[2])
                .thenAccept(outcome -> msg.send(player, outcome.key(), outcome.replacements()));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 3) {
            return ArenaRegistry.names().stream().toList();
        }
        return null;
    }
}
