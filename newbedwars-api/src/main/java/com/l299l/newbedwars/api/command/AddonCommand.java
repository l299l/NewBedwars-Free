package com.l299l.newbedwars.api.command;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** A {@code /bw <name>} subcommand added by an addon. It is listed in {@code /bw help} like the built-in ones. */
public interface AddonCommand {
    /** Name after {@code /bw}; must not clash with another subcommand available in the same contexts. */
    String getName();

    String getDescription();

    String getSyntax();

    String getExample();

    /**
     * @param args the whole argument list; {@code args[0]} is the command name
     * @param arena the arena the player is in or sets up, or {@code null} outside arenas
     */
    void perform(Player player, String[] args, ArenaView arena);

    default List<String> tabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Runs the command from the console (or a command block). Called only when the command is registered
     * for {@link CommandContext#CONSOLE}. Does nothing by default. Available from 2.7-beta.
     *
     * @param args the whole argument list; {@code args[0]} is the command name
     */
    default void performConsole(CommandSender sender, String[] args) {
    }

    /** Tab completion in the console; see {@link #performConsole(CommandSender, String[])}. Available from 2.7-beta. */
    default List<String> tabCompleteConsole(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
