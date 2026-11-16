package com.l299l.newbedwars.api.view;

/** A snapshot of a player's saved stats. It doesn't change when the stats do. */
public interface PlayerStatsView {
    int getWins();

    int getLosses();

    int getKills();

    int getDeaths();

    int getFinalKills();

    int getBedsBroken();

    int getGamesPlayed();
}
