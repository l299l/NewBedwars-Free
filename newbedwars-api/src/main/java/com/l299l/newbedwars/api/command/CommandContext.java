package com.l299l.newbedwars.api.command;

/** Where a {@code /bw} subcommand is available. */
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
    /** While setting up an arena in normal and advanced setup. */
    SETUP_NORMAL,
    /** While setting up an arena in advanced setup. */
    SETUP_ADVANCED,
    /** While setting up an arena in automatic setup. */
    SETUP_AUTOMATIC,
    /**
     * Same as {@link #SETUP_AUTOMATIC}.
     *
     * @deprecated renamed, use {@link #SETUP_AUTOMATIC}
     */
    @Deprecated
    SETUP_BUILDING
}
