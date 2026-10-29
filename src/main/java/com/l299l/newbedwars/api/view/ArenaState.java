package com.l299l.newbedwars.api.view;

/** Lifecycle state of an arena. */
public enum ArenaState {
    WAITING,
    STARTING,
    PLAYING,
    ENDING,
    /** The map is being reset; nobody can join. */
    RESTARTING
}
