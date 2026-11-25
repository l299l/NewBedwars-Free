package com.l299l.newbedwars.api.command;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** A {@code /bw <name>} subcommand added by an addon. It is listed in {@code /bw help} like the built-in ones. */
public interface AddonCommand {
    /** The name after {@code /bw}. It must not clash with another subcommand in the same contexts. */
    String getName();

    /**
     * Shown in {@code /bw help}. Server owners can replace it with a {@code HelpDesc-<name>} key in the
     * language files.
     */
    String getDescription();

    /** Usage shown in {@code /bw help}, e.g. {@code /bw mycommand <player>}. */
    String getSyntax();

    /** Shown when hovering the command in {@code /bw help}. */
    String getExample();

    /**
     * Runs the command for a player, on the server thread.
     *
     * @param args  all arguments, {@code args[0]} is the command name
     * @param arena the arena the player is in or setting up, or {@code null} outside arenas
     */
    void perform(Player player, String[] args, ArenaView arena);

    /** Suggestions for the argument being typed, the last one in {@code args}. */
    default List<String> tabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Permission needed to use this command when it is registered in an admin or setup context
     * ({@code LOBBY_ADMIN}, {@code GAME_ADMIN}, {@code SETUP_*}). Admins ({@code newbedwars.bw.admin} or
     * operators) always can. With {@code null}, admin commands are for admins only and setup commands are
     * open to every player who is setting up the arena. Ignored in public contexts.
     *
     * @since 2.8-beta
     */
    default String permission() {
        return null;
    }

    /**
     * Runs the command from the console or a command block. Only called when the command is registered for
     * {@link CommandContext#CONSOLE}. Does nothing by default.
     *
     * @param args all arguments, {@code args[0]} is the command name
     * @since 2.7-beta
     */
    default void performConsole(CommandSender sender, String[] args) {
    }

    /**
     * Tab completion in the console, for {@link #performConsole(CommandSender, String[])}.
     *
     * @since 2.7-beta
     */
    default List<String> tabCompleteConsole(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
