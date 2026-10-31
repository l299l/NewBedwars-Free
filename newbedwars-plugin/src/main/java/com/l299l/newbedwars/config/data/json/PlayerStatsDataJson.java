package com.l299l.newbedwars.config.data.json;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.JsonParseException;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.player.PlayerStats;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Level;

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
        } catch (IOException | JsonParseException e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not read playerStats.json; starting without saved stats", e);
        }
    }

    /**
     * Serializes the stats on the calling (main) thread, so nothing changes them while they are read,
     * and writes the file in the background through a temp file, so a crash can not leave it half written.
     */
    public void save() {
        String json = new Gson().toJson(new HashMap<>(NewBedwars.plugin.getPlayerManager().getAllStats()));
        Path target = new File(NewBedwars.plugin.getDataFolder(), "data/playerStats.json").toPath();
        Runnable write = () -> {
            try {
                AtomicFileWriter.write(target, json, null, 0);
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not write " + target, e);
            }
        };
        if (NewBedwars.plugin.isEnabled()) {
            IOExecutor.submit(write);
        } else {
            write.run();
        }
    }

    private int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }
}
