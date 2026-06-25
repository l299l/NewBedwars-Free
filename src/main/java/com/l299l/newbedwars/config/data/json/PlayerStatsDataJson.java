package com.l299l.newbedwars.config.data.json;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.player.PlayerStats;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class PlayerStatsDataJson {

    public void load() {
        File statsFile = new File(NewBedwars.plugin.getDataFolder(), "data/playerStats.json");
        if (!statsFile.exists()) {
            save();
            return;
        }
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(statsFile)) {
            Object obj = gson.fromJson(reader, HashMap.class);
            if (obj == null) return;
            HashMap<String, LinkedTreeMap> raw = (HashMap<String, LinkedTreeMap>) obj;
            for (String name : raw.keySet()) {
                LinkedTreeMap<String, Object> m = raw.get(name);
                PlayerStats stats = new PlayerStats(
                        toInt(m.get("wins")),
                        toInt(m.get("losses")),
                        toInt(m.get("kills")),
                        toInt(m.get("deaths")),
                        toInt(m.get("finalKills")),
                        toInt(m.get("bedsBroken")),
                        toInt(m.get("gamesPlayed"))
                );
                NewBedwars.plugin.getPlayerManager().addStats(name, stats);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        Gson gson = new Gson();
        File dataDir = new File(NewBedwars.plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        try (FileWriter writer = new FileWriter(new File(dataDir, "playerStats.json"))) {
            gson.toJson(NewBedwars.plugin.getPlayerManager().getAllStats(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }
}
