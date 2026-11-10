package com.l299l.newbedwars.api.maintenance;

/**
 * Result of an {@link ArenaMaintenance} action.
 *
 * @param code    what happened
 * @param arena   the arena (or, for an import, the file) the action was about
 * @param message the message an admin would get for the same command, in the server's default language, without colors
 * @param detail  extra information or {@code null}: the backup id, the export file, the copy source or the error
 */
public record MaintenanceResult(Code code, String arena, String message, String detail) {

    public enum Code {
        /** Done (for {@code resetMap}: the reset was started). */
        OK,
        /** The arena, backup or import file does not exist. */
        NOT_FOUND,
        /** Players are in the arena or a game is running. */
        BUSY,
        /** The arena is already enabled / disabled. */
        ALREADY_DONE,
        /** The arena can not be enabled because its setup is not complete. */
        NOT_READY,
        /** The new arena name may only use letters, digits, {@code _} and {@code -}. */
        INVALID_NAME,
        /** An arena, map or world with that name already exists. */
        NAME_TAKEN,
        /** Not possible on this server (a {@code LOBBY} server has no arenas). */
        UNAVAILABLE,
        /** An error; see {@link #detail()} and the console. */
        FAILED,
        /** The arena was deleted, but some files could not be removed. */
        PARTIAL
    }

    public boolean isSuccess() {
        return code == Code.OK;
    }
}
