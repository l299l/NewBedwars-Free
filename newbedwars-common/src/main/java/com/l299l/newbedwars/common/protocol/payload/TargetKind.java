package com.l299l.newbedwars.common.protocol.payload;

/** What a join request aims at. */
public enum TargetKind {
    /** Any arena of the network. */
    ANY,
    /** The best arena of an arena group (or name pattern / team size, like local group joins). */
    GROUP,
    /** One named arena. */
    ARENA,
    /** Back into the running game the player left (the proxy finds it from the rejoin slots). */
    REJOIN,
    /** Watch a running game (an arena name) as a spectator. */
    SPECTATE
}
