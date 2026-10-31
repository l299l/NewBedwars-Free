package com.l299l.newbedwars.apiimpl.adapter;

import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.api.command.AddonCommand;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import org.bukkit.entity.Player;

import java.util.List;

/** Shows an addon's command to the plugin's command system. */
public final class ApiSubCommand extends SubCommand {
    private final AddonCommand command;

    public ApiSubCommand(AddonCommand command) {
        this.command = command;
    }

    @Override
    public String getName() {
        return command.getName();
    }

    @Override
    public String getDescription() {
        return command.getDescription();
    }

    @Override
    public String getSyntax() {
        return command.getSyntax();
    }

    @Override
    public String getExample() {
        return command.getExample();
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        command.perform(player, args, arena == null ? null : new ApiArena(arena));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return command.tabComplete(player, args);
    }
}
