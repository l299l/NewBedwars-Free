package com.l299l.newbedwars.apiimpl.view;

import com.l299l.newbedwars.api.view.PlayerStatsView;
import com.l299l.newbedwars.player.PlayerStats;

public record ApiPlayerStats(PlayerStats stats) implements PlayerStatsView {
    @Override public int getWins() { return stats.wins(); }
    @Override public int getLosses() { return stats.losses(); }
    @Override public int getKills() { return stats.kills(); }
    @Override public int getDeaths() { return stats.deaths(); }
    @Override public int getFinalKills() { return stats.finalKills(); }
    @Override public int getBedsBroken() { return stats.bedsBroken(); }
    @Override public int getGamesPlayed() { return stats.gamesPlayed(); }
}
