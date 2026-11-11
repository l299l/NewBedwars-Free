package com.l299l.newbedwars.apiimpl.adapter;

import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.api.command.AddonCommand;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

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

    @Override
    public void performConsole(CommandSender sender, String[] args) {
        command.performConsole(sender, args);
    }

    @Override
    public List<String> getConsoleArguments(CommandSender sender, String[] args) {
        return command.tabCompleteConsole(sender, args);
    }
}
