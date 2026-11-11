package com.l299l.newbedwars.api.view;

/** A player statistic that can be ranked. Available from 2.7-beta. */
public enum StatType {
    WINS,
    LOSSES,
    KILLS,
    DEATHS,
    FINAL_KILLS,
    BEDS_BROKEN,
    GAMES_PLAYED;

    /** The value of this statistic in {@code stats}. */
    public int of(PlayerStatsView stats) {
        switch (this) {
            case WINS: return stats.getWins();
            case LOSSES: return stats.getLosses();
            case KILLS: return stats.getKills();
            case DEATHS: return stats.getDeaths();
            case FINAL_KILLS: return stats.getFinalKills();
            case BEDS_BROKEN: return stats.getBedsBroken();
            default: return stats.getGamesPlayed();
        }
    }
}
