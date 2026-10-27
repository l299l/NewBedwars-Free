package com.l299l.newbedwars.api.events;

/** Why a player left an arena. */
public enum LeaveReason {
    /** The player used a command or a menu to leave. */
    COMMAND,
    /** The player left the server. */
    DISCONNECT,
    /** The player was removed by the plugin (arena disabled, deleted or reset). */
    KICKED,
    OTHER
}
