package com.l299l.newbedwars.api.maintenance;

/**
 * Result of an {@link ArenaMaintenance} or {@link com.l299l.newbedwars.api.game.GameControl} action.
 *
 * @param code    what happened
 * @param arena   the arena the action was about; for an import, the file
 * @param message what an admin would see for the same command, in the server's default language, without
 *                colors
 * @param detail  depends on the action (see its method), {@code null} when there is nothing to add
 * @since 2.7-beta
 */
public record MaintenanceResult(Code code, String arena, String message, String detail) {

    public enum Code {
        /** Done. For {@code resetMap}: the reset has started. */
        OK,
        /** The arena, backup or import file does not exist. */
        NOT_FOUND,
        /** Players are in the arena or a game is running. */
        BUSY,
        /** The arena is already enabled or disabled. */
        ALREADY_DONE,
        /**
         * Not possible in the arena's current state: the setup is not complete (enable), or the game is not in
         * the right state ({@link com.l299l.newbedwars.api.game.GameControl}).
         */
        NOT_READY,
        /**
         * An argument is not valid: an arena name with characters other than letters, digits, {@code _} and
         * {@code -}, or a broadcast message that is empty or too long.
         */
        INVALID_NAME,
        /** An arena, map or world with that name already exists. */
        NAME_TAKEN,
        /** Not possible on this server, e.g. a {@code LOBBY} server has no arenas. */
        UNAVAILABLE,
        /** Something went wrong; see {@link #detail()} and the console. */
        FAILED,
        /** The arena was deleted, but some files couldn't be removed. */
        PARTIAL
    }

    public boolean isSuccess() {
        return code == Code.OK;
    }
}
