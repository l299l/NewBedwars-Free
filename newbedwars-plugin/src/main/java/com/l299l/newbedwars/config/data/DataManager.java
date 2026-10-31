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
    /** Only for {@code Storage.Type: MYSQL}. */
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
        // JSON files are always read: they are the data in JSON mode and what gets imported into an empty database.
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

    /** The MySQL player storage, or null with {@code Storage.Type: JSON}. */
    public PlayerDataSql getPlayerDataSql() {
        return playerDataSql;
    }

    /** Stops background tasks before this manager is replaced ({@code /bw reload}). */
    public void stop() {
        if (playerDataSql != null) playerDataSql.stop();
    }

    /** A player's language or quick buy changed. */
    public void playerChanged(String name) {
        if (playerDataSql != null) playerDataSql.savePlayer(name);
    }

    /** A game ended for a player; in MySQL mode the result is added in the database (JSON is saved as a whole). */
    public void recordGameResult(String name, com.l299l.newbedwars.player.PlayerStats delta) {
        if (playerDataSql != null) {
            playerDataSql.recordGame(name, delta);
            playerDataSql.refreshLeaderboards();
        }
    }

    public void loadArenas() {
        if (com.l299l.newbedwars.config.properties.Properties.NetworkMode == com.l299l.newbedwars.api.view.NetworkMode.LOBBY) {
            // A lobby server of a network has no arenas; they live on the game servers.
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
        itemsDataManager.save();
        guiDataManager.save();
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

    public List<GUISave> getGuiData() {
        return guiDataManager.getGuiSaves();
    }
}
