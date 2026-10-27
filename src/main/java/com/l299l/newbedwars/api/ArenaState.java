package com.l299l.newbedwars.api;

/** Lifecycle state of an arena, mirrors the internal game status. */
public enum ArenaState {
    WAITING,
    STARTING,
    PLAYING,
    ENDING,
    /** The map is being reset; nobody can join. */
    RESTARTING
}
