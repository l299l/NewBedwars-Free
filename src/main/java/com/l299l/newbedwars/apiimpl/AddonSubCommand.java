package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.AddonCommand;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import org.bukkit.entity.Player;

import java.util.List;

/** Shows an addon's command to the plugin's command system. */
final class AddonSubCommand extends SubCommand {
    private final AddonCommand command;

    AddonSubCommand(AddonCommand command) {
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
