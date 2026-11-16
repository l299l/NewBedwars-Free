package com.l299l.newbedwars.api.view;

import java.util.Optional;

/**
 * A snapshot of an arena on a game server, as the proxy last reported it. Available on
 * {@link NetworkMode#LOBBY} and {@link NetworkMode#GAME} servers.
 */
public interface NetworkArenaView {
    /** The game server's name in the proxy. */
    String getServer();

    String getName();

    /** The arena group, if the arena is in one. */
    Optional<String> getGroup();

    /** Empty when the arena is disabled. */
    Optional<ArenaState> getState();

    int getPlayerCount();

    int getMaxPlayers();

    int getMaxInTeam();

    int getTeamCount();

    /** Whether players can join, i.e. it is waiting or counting down. */
    default boolean isJoinable() {
        Optional<ArenaState> s = getState();
        return s.isPresent() && (s.get() == ArenaState.WAITING || s.get() == ArenaState.STARTING);
    }

    /** {@code max - players}, never negative. */
    default int getFreeSlots() {
        return Math.max(0, getMaxPlayers() - getPlayerCount());
    }
}
