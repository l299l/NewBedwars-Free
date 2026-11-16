package com.l299l.newbedwars.api.command;

/**
 * Where a {@code /bw} subcommand is available. Arena setup has three modes that admins pick from: normal,
 * advanced (normal plus more settings) and automatic.
 */
public enum CommandContext {
    /** Outside any arena, for everyone. */
    LOBBY,
    /** Outside any arena, for admins. */
    LOBBY_ADMIN,
    /** Inside a game or spectating, for everyone. */
    GAME,
    /** Inside a game or spectating, for admins. */
    GAME_ADMIN,
    /** While setting up an arena, in every setup mode. */
    SETUP_BASIC,
    /** While setting up an arena in normal or advanced mode. */
    SETUP_NORMAL,
    /** While setting up an arena in advanced mode. */
    SETUP_ADVANCED,
    /** While setting up an arena in automatic mode. */
    SETUP_AUTOMATIC,
    /**
     * Typed in the server console or run by a command block. The command runs
     * {@link AddonCommand#performConsole(org.bukkit.command.CommandSender, String[])}.
     *
     * @since 2.7-beta
     */
    CONSOLE,
    /**
     * Same as {@link #SETUP_AUTOMATIC}.
     *
     * @deprecated renamed, use {@link #SETUP_AUTOMATIC}
     */
    @Deprecated
    SETUP_BUILDING
}
