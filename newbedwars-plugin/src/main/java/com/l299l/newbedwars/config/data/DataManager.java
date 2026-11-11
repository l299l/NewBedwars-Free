package com.l299l.newbedwars.config.data;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.config.StorageType;
import com.l299l.newbedwars.config.data.json.PlayerDataJson;
import com.l299l.newbedwars.config.data.json.PlayerStatsDataJson;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataManager;
import com.l299l.newbedwars.config.data.sql.PlayerDataSql;
import com.l299l.newbedwars.config.data.yaml.guis.GUIDataManager;
import com.l299l.newbedwars.config.data.yaml.items.ItemsDataManager;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUISave;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;

public class DataManager {
    private final StorageType storageType;
    private final PlayerDataJson playerDataJson;
    private final PlayerStatsDataJson playerStatsDataJson;

    private final PlayerDataSql playerDataSql;
    private final ArenaDataManager arenaDataManager;
    private final ItemsDataManager itemsDataManager;
    private final GUIDataManager guiDataManager;

    public DataManager() {
        arenaDataManager = new ArenaDataManager();
        itemsDataManager = new ItemsDataManager();
        guiDataManager = new GUIDataManager();
        playerStatsDataJson = new PlayerStatsDataJson();
        this.storageType = Properties.StorageType;

        playerDataJson = new PlayerDataJson();
        playerDataSql = storageType == StorageType.MYSQL ? new PlayerDataSql(com.l299l.newbedwars.NewBedwars.plugin) : null;
    }

    public void load() {
        playerDataJson.load();
        playerStatsDataJson.load();
        if (playerDataSql != null) playerDataSql.load();
        itemsDataManager.load();
        guiDataManager.load();
    }

    public PlayerDataSql getPlayerDataSql() {
        return playerDataSql;
    }

    public void stop() {
        if (playerDataSql != null) playerDataSql.stop();
    }

    public void playerChanged(String name) {
        if (playerDataSql != null) playerDataSql.savePlayer(name);
    }

    public void recordGameResult(String name, com.l299l.newbedwars.player.PlayerStats delta) {
        if (playerDataSql != null) {
            playerDataSql.recordGame(name, delta);
            playerDataSql.refreshLeaderboards();
        }
    }

    public void loadArenas() {
        if (com.l299l.newbedwars.config.properties.Properties.NetworkMode == com.l299l.newbedwars.api.view.NetworkMode.LOBBY) {

            Bukkit.getLogger().info("[NewBedwars] Network.Mode is LOBBY: arenas are not loaded on this server");
            return;
        }
        arenaDataManager.load();
    }
    public ArenaDataManager getArenaDataManager() {
        return arenaDataManager;
    }

    public void unloadArenas() {
        for (World arenaWorld : ArenaRegistry.worlds()) {
            boolean correct = Bukkit.getServer().unloadWorld(arenaWorld, false);
            if (!correct) {
                Bukkit.getServer().getLogger().severe("Could not unload arena world " + arenaWorld.getName());
            }
        }
    }

    public void save() {
        savePlayerData();
        savePlayerStats();
        arenaDataManager.save();
        unloadArenas();
    }

    public void savePlayerData() {
        if (playerDataSql != null) {
            playerDataSql.saveOnlinePlayers();
        } else {
            playerDataJson.save();
        }
    }

    public void savePlayerStats() {
        if (playerDataSql == null) playerStatsDataJson.save();
    }

    public ItemsDataManager getItemsDataManager() {
        return itemsDataManager;
    }

    public GUIDataManager getGuiDataManager() {
        return guiDataManager;
    }

    public List<GUISave> getGuiData() {
        return guiDataManager.getGuiSaves();
    }
}
