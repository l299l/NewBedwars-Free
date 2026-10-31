package com.l299l.newbedwars.api.view;

/** How this server takes part in a proxy network ({@code Network.Mode} in config.yml). */
public enum NetworkMode {
    /** A single server (no network). */
    NONE,
    /** Hosts arenas; players come from and return to lobby servers. */
    GAME,
    /** Has no arenas; sends players to game servers. */
    LOBBY
}
