package com.l299l.newbedwars.api.view;

import java.util.Optional;

/**
 * An arena on a game server of the network, as last reported to this server (read-only snapshot).
 * Only filled on {@link NetworkMode#LOBBY} servers.
 */
public interface NetworkArenaView {
    /** The game server's name in the proxy config. */
    String getServer();

    String getName();

    /** The arena group, if the arena is in one. */
    Optional<String> getGroup();

    /** The state; empty when the arena is disabled. */
    Optional<ArenaState> getState();

    int getPlayerCount();

    int getMaxPlayers();

    int getMaxInTeam();

    int getTeamCount();

    /** Whether new players can join (waiting or starting). */
    default boolean isJoinable() {
        Optional<ArenaState> s = getState();
        return s.isPresent() && (s.get() == ArenaState.WAITING || s.get() == ArenaState.STARTING);
    }

    /** Places left ({@code max - players}, never negative). */
    default int getFreeSlots() {
        return Math.max(0, getMaxPlayers() - getPlayerCount());
    }
}
