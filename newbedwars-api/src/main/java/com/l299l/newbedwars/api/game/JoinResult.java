package com.l299l.newbedwars.api.game;

/**
 * The result of {@link com.l299l.newbedwars.api.NewBedwarsAPI#join} and
 * {@link com.l299l.newbedwars.api.NewBedwarsAPI#spectate}. The player also gets the same chat message as with
 * the matching command.
 *
 * @since 2.8-beta
 */
public enum JoinResult {
    /** The player (and their party) joined the waiting lobby of the arena. */
    JOINED,
    /** The player went back into the game they had left ({@code /bw rejoin}). */
    REJOINED,
    /** The player is now spectating. */
    SPECTATING,
    /** The player is in a party and is not its leader; only the leader can join. */
    NOT_PARTY_LEADER,
    /** The player is already in this arena. */
    ALREADY_IN_ARENA,
    /** The arena is disabled. */
    NOT_ENABLED,
    /** The arena has no free place for the player or their party. */
    FULL,
    /** The party has more members than a team has places. */
    PARTY_TOO_BIG,
    /** The arena doesn't allow parties ({@code AllowParties}). */
    PARTIES_NOT_ALLOWED,
    /** The game is running; join it as a spectator instead. */
    RUNNING,
    /** The game is ending. */
    ENDING,
    /** The map is being reset. */
    RESTARTING,
    /** Spectating: no game is running in the arena. */
    NOT_RUNNING,
    /** Spectating: the arena doesn't allow spectators ({@code AllowSpectators}). */
    SPECTATORS_DISABLED,
    /** An addon cancelled it ({@code PlayerJoinArenaEvent} or {@code PlayerSpectateEvent}). */
    CANCELLED,
    /** It failed for another reason, e.g. a rejoin whose place is gone, or the player is playing in another arena. */
    FAILED;

    /** Whether the player is now in the arena (playing, waiting or spectating). */
    public boolean isSuccess() {
        return this == JOINED || this == REJOINED || this == SPECTATING;
    }
}
