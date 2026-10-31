package com.l299l.newbedwars.player;

public record PlayerStats(int wins, int losses, int kills, int deaths, int finalKills, int bedsBroken, int gamesPlayed) {

    public static PlayerStats empty() {
        return new PlayerStats(0, 0, 0, 0, 0, 0, 0);
    }

    public PlayerStats addGameResult(int k, int d, int fk, int bb, boolean won) {
        return new PlayerStats(
                wins + (won ? 1 : 0),
                losses + (won ? 0 : 1),
                kills + k,
                deaths + d,
                finalKills + fk,
                bedsBroken + bb,
                gamesPlayed + 1
        );
    }
}
