package com.l299l.newbedwars.config.data.mysql.models;

import com.l299l.newbedwars.config.Language;
import jakarta.persistence.*;

@Entity
@Table(name = "Players")
public class PlayerModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String playerName;

    @Column(name = "lang")
    @Enumerated(EnumType.STRING)
    private Language language;

    @Column(name = "shopGui")
    private String shopGui;

    @Column(name = "upgradeGui")
    private String upgradeGui;

    public PlayerModel(String playerName, Language language, String shopGui, String upgradeGui) {
        super();
        this.playerName = playerName;
        this.language = language;
        this.shopGui = shopGui;
        this.upgradeGui = upgradeGui;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return playerName;
    }

    public void setName(String playerName) {
        this.playerName = playerName;
    }

    public Language getLang() {
        return language;
    }

    public void setLang(Language language) {
        this.language = language;
    }

    public String getShopGui() {
        return shopGui;
    }

    public void setShopGui(String shopGui) {
        this.shopGui = shopGui;
    }

    public String getUpgradeGui() {
        return upgradeGui;
    }

    public void setUpgradeGui(String upgradeGui) {
        this.upgradeGui = upgradeGui;
    }
}
