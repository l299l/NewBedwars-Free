package com.l299l.newbedwars.config.data;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.config.StorageType;
import com.l299l.newbedwars.config.data.json.PlayerDataJson;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataManager;
import com.l299l.newbedwars.config.data.mysql.LangDataMySQL;
import com.l299l.newbedwars.config.data.mysql.MySQLManager;
import com.l299l.newbedwars.config.data.yaml.guis.GuiDataManager;
import com.l299l.newbedwars.config.data.yaml.items.ItemsDataManager;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GuiSave;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;

public class DataManager {
    private final StorageType storageType;
    private final PlayerDataJson playerDataJson;
    private final LangDataMySQL langDataMySQL;
    private final ArenaDataManager arenaDataManager;
    private final ItemsDataManager itemsDataManager;
    private final GuiDataManager guiDataManager;
    private MySQLManager mySQLManager;

    public DataManager() {
        arenaDataManager = new ArenaDataManager();
        itemsDataManager = new ItemsDataManager();
        guiDataManager = new GuiDataManager();
        mySQLManager = null;
        this.storageType = Properties.StorageType;
        if (storageType == StorageType.JSON) {
            playerDataJson = new PlayerDataJson();
            langDataMySQL = null;
        } else {
            mySQLManager = new MySQLManager();
            playerDataJson = null;
            langDataMySQL = new LangDataMySQL(mySQLManager);
        }
    }

    public void load() {
        if (storageType == StorageType.JSON) {
            if (playerDataJson == null) throw new IllegalStateException("playerDataJson is null in JSON storage mode");
            playerDataJson.load();
        } else if (storageType == StorageType.MYSQL) {
            if (langDataMySQL == null) throw new IllegalStateException("langDataMySQL is null in MYSQL storage mode");
            langDataMySQL.load();
        }
        itemsDataManager.load();
        guiDataManager.load();
    }

    public void loadArenas() {
        arenaDataManager.load();
    }
    public void unloadArenas() {
        for (World arenaWorld : Arena.arenaByWorld.keySet()) {
            boolean correct = Bukkit.getServer().unloadWorld(arenaWorld, false);
            if (!correct) {
                Bukkit.getServer().getLogger().severe("Could not unload arena world " + arenaWorld.getName());
            }
        }
    }

    public void save() {
        savePlayerData();
        arenaDataManager.save();
        unloadArenas();
        itemsDataManager.save();
        guiDataManager.save();
    }

    public void savePlayerData() {
        if (storageType == StorageType.JSON) {
            if (playerDataJson == null) throw new IllegalStateException("playerDataJson is null in JSON storage mode");
            playerDataJson.save();
        } else if (storageType == StorageType.MYSQL) {
            if (langDataMySQL == null) throw new IllegalStateException("langDataMySQL is null in MYSQL storage mode");
            langDataMySQL.save();
        }
    }

    public List<GuiSave> getGuiData() {
        return guiDataManager.getGuiSaves();
    }
}
