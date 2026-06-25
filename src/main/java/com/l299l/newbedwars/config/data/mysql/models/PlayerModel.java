package com.l299l.newbedwars.config.data.mysql.models;

import com.l299l.newbedwars.config.Language;

public class PlayerModel {
    private Long id;
    private String playerName;
    private Language language;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int finalKills;
    private int bedsBroken;
    private int gamesPlayed;

    public PlayerModel(String playerName, Language language) {
        this.playerName = playerName;
        this.language = language;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return playerName; }
    public void setName(String playerName) { this.playerName = playerName; }

    public Language getLang() { return language; }
    public void setLang(Language language) { this.language = language; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }

    public int getFinalKills() { return finalKills; }
    public void setFinalKills(int finalKills) { this.finalKills = finalKills; }

    public int getBedsBroken() { return bedsBroken; }
    public void setBedsBroken(int bedsBroken) { this.bedsBroken = bedsBroken; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
}
