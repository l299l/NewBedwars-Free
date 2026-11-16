package com.l299l.newbedwars.api.events;

/** Why a player left an arena. */
public enum LeaveReason {
    /** The player used a command or a menu to leave. */
    COMMAND,
    /** The player left the server. */
    DISCONNECT,
    /** The plugin removed the player, e.g. the arena was disabled, deleted or force-stopped. */
    KICKED,
    /** Anything else, e.g. the game ended and everyone was sent out. */
    OTHER
}
