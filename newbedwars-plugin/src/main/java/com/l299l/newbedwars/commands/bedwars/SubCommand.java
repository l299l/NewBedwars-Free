package com.l299l.newbedwars.commands.bedwars;

import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class SubCommand {
    public abstract String getName();

    public abstract String getDescription();

    public abstract String getSyntax();

    public abstract String getExample();

    public abstract void perform(Player player, String[] args, IArena arena);

    public abstract List<String> getSubcommandArguments(Player player, String[] args);

}
