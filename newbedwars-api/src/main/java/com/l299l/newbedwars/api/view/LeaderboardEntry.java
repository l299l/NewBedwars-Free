package com.l299l.newbedwars.api.view;

/** One row of a leaderboard: the player's name and all their statistics. Available from 2.7-beta. */
public final class LeaderboardEntry {
    private final String name;
    private final PlayerStatsView stats;

    public LeaderboardEntry(String name, PlayerStatsView stats) {
        this.name = name;
        this.stats = stats;
    }

    public String getName() {
        return name;
    }

    public PlayerStatsView getStats() {
        return stats;
    }
}
