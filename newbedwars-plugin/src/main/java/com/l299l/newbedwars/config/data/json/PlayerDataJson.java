package com.l299l.newbedwars.config.data.json;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.player.PlayerProfile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class PlayerDataJson {
    private final AtomicReference<String> pending = new AtomicReference<>();
    private boolean saveBlocked;

    public void load(){
        File playerFile = new File(NewBedwars.plugin.getDataFolder(), "data/playersData.json");
        if (!playerFile.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(playerFile)) {
            Object obj = new Gson().fromJson(reader, HashMap.class);
            if (obj == null) return;
            HashMap<String, PlayerProfile> hashPlayers = castToHashMap((HashMap<String, LinkedTreeMap>) obj);
            for (PlayerProfile profile : hashPlayers.values()) {
                NewBedwars.plugin.getPlayerManager().addPlayer(profile);
            }
        } catch (IOException | RuntimeException e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not read " + playerFile.getPath() + "; player data will not be saved over it", e);
            saveBlocked = !moveAside(playerFile);
        }
    }

    private boolean moveAside(File file) {
        File target = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
        try {
            Files.move(file.toPath(), target.toPath());
            NewBedwars.plugin.getLogger().severe("Moved the unreadable file to " + target.getPath());
            return true;
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not move " + file.getPath() + " aside; saving player data is disabled", e);
            return false;
        }
    }

    public void save() {
        if (saveBlocked) return;
        String json = new Gson().toJson(new HashMap<>(NewBedwars.plugin.getPlayerManager().getPlayers()));
        boolean schedule = pending.getAndSet(json) == null;
        if (!schedule) return;
        if (NewBedwars.plugin.isEnabled()) {
            IOExecutor.submit(this::flushPending);
        } else {
            flushPending();
        }
    }

    private void flushPending() {
        String json = pending.getAndSet(null);
        if (json == null) return;
        Path target = new File(NewBedwars.plugin.getDataFolder(), "data/playersData.json").toPath();
        try {
            AtomicFileWriter.write(target, json, null, 0);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not write " + target, e);
        }
    }

    private HashMap<String, PlayerProfile> castToHashMap(HashMap<String, LinkedTreeMap> hashMap) {
        HashMap<String, PlayerProfile> newHashMap = new HashMap<>();
        for (String key : hashMap.keySet()) {
            LinkedTreeMap<String, Object> player = hashMap.get(key);
            Map<String, List<String>> fastBuyPerCat = new HashMap<>();
            Object rawFastBuy = player.get("fastBuyPerCategory");
            if (rawFastBuy instanceof LinkedTreeMap<?, ?> catMap) {
                for (Map.Entry<?, ?> entry : catMap.entrySet()) {
                    if (!(entry.getKey() instanceof String catId)) continue;
                    List<String> items = new ArrayList<>();
                    if (entry.getValue() instanceof List<?> list) {
                        for (Object o : list) { if (o instanceof String s) items.add(s); }
                    }
                    fastBuyPerCat.put(catId, items);
                }
            }
            newHashMap.put(key, new PlayerProfile(
                    UUID.fromString((String) player.get("id")),
                    (String) player.get("name"),
                    Language.valueOf((String) player.get("language")),
                    fastBuyPerCat
            ));
        }
        return newHashMap;
    }
}
