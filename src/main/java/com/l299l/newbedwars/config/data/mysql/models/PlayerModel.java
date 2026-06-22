package com.l299l.newbedwars.config.data.mysql.models;

import com.l299l.newbedwars.config.Language;

public class PlayerModel {
    private Long id;
    private String playerName;
    private Language language;
    private String shopGui;
    private String upgradeGui;

    public PlayerModel(String playerName, Language language, String shopGui, String upgradeGui) {
        this.playerName = playerName;
        this.language = language;
        this.shopGui = shopGui;
        this.upgradeGui = upgradeGui;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return playerName; }
    public void setName(String playerName) { this.playerName = playerName; }

    public Language getLang() { return language; }
    public void setLang(Language language) { this.language = language; }

    public String getShopGui() { return shopGui; }
    public void setShopGui(String shopGui) { this.shopGui = shopGui; }

    public String getUpgradeGui() { return upgradeGui; }
    public void setUpgradeGui(String upgradeGui) { this.upgradeGui = upgradeGui; }
}
