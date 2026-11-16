package com.l299l.newbedwars.api.view;

public enum ArenaState {
    WAITING,
    STARTING,
    PLAYING,
    ENDING,
    /** The map is being reset; nobody can join. */
    RESTARTING
}
