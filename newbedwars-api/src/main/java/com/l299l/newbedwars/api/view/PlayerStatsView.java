package com.l299l.newbedwars.api.view;

/** A player's saved stats (read-only snapshot). */
public interface PlayerStatsView {
    int getWins();

    int getLosses();

    int getKills();

    int getDeaths();

    int getFinalKills();

    int getBedsBroken();

    int getGamesPlayed();
}
