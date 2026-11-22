package com.l299l.newbedwars.config.data.json;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerStats;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerStatsDataJson {

    private boolean saveBlocked;

    public void load() {
        File statsFile = new File(NewBedwars.plugin.getDataFolder(), "data/playerStats.json");
        if (!statsFile.exists()) {
            save();
            return;
        }
        int legacy;
        PlayerManager pm = NewBedwars.plugin.getPlayerManager();
        Map<UUID, PlayerStats> before = new HashMap<>(pm.getAllStats());
        try (FileReader reader = new FileReader(statsFile)) {
            Object obj = new Gson().fromJson(reader, HashMap.class);
            if (obj == null) return;
            pm.getAllStats().clear();
            legacy = read((Map<String, LinkedTreeMap<String, Object>>) obj, pm);
        } catch (IOException | RuntimeException e) {
            pm.getAllStats().clear();
            pm.getAllStats().putAll(before);
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not read playerStats.json; stats are not saved over it", e);
            saveBlocked = !com.l299l.newbedwars.config.data.CorruptFiles.moveAside(statsFile, NewBedwars.plugin.getLogger(),
                    "saving stats is disabled");
            return;
        }
        if (legacy > 0) {
            Path backup = statsFile.toPath().resolveSibling("playerStats.json.before-uuid");
            try {
                if (!Files.exists(backup)) Files.copy(statsFile.toPath(), backup);
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not back up playerStats.json", e);
            }
            NewBedwars.plugin.getLogger().info("Moved the stats of " + legacy + " players from names to UUIDs (old file kept as "
                    + backup.getFileName() + ")");
            save();
        }
    }

    static int read(Map<String, LinkedTreeMap<String, Object>> raw, PlayerManager pm) {
        int legacy = 0;
        for (Map.Entry<String, LinkedTreeMap<String, Object>> entry : raw.entrySet()) {
            Map<String, Object> m = entry.getValue();
            if (m == null) continue;
            PlayerStats stats = new PlayerStats(
                    toInt(m.get("wins")),
                    toInt(m.get("losses")),
                    toInt(m.get("kills")),
                    toInt(m.get("deaths")),
                    toInt(m.get("finalKills")),
                    toInt(m.get("bedsBroken")),
                    toInt(m.get("gamesPlayed"))
            );
            UUID id = parseUuid(entry.getKey());
            if (id != null) {
                pm.mergeStats(id, m.get("name") instanceof String n ? n : null, stats);
                continue;
            }
            String name = entry.getKey();
            UUID known = pm.idOf(name);
            pm.mergeStats(known != null ? known : PlayerManager.offlineId(name), name, stats);
            legacy++;
        }
        return legacy;
    }

    private static UUID parseUuid(String s) {
        try {
            return s.length() == 36 ? UUID.fromString(s) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Map<String, Map<String, Object>> toFile(PlayerManager pm) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerStats> e : pm.getAllStats().entrySet()) {
            PlayerStats s = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            String name = pm.nameOf(e.getKey());
            if (name != null) row.put("name", name);
            row.put("wins", s.wins());
            row.put("losses", s.losses());
            row.put("kills", s.kills());
            row.put("deaths", s.deaths());
            row.put("finalKills", s.finalKills());
            row.put("bedsBroken", s.bedsBroken());
            row.put("gamesPlayed", s.gamesPlayed());
            out.put(e.getKey().toString(), row);
        }
        return out;
    }

    public void save() {
        if (saveBlocked) return;
        String json = new Gson().toJson(toFile(NewBedwars.plugin.getPlayerManager()));
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

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }
}
