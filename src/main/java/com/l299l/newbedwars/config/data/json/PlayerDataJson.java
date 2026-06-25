package com.l299l.newbedwars.config.data.json;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.player.PlayerIns;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataJson {

    public void load(){
        File playerFile = new File(NewBedwars.plugin.getDataFolder(), "data/playersData.json");
        if (playerFile.exists()) {
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(playerFile)) {
                Object obj = gson.fromJson(reader, HashMap.class);
                HashMap<String, PlayerIns> hashPlayers = castToHashMap((HashMap<String, LinkedTreeMap>) obj);
                for (String key: hashPlayers.keySet()) {
                    NewBedwars.plugin.getPlayerManager().addPlayer(hashPlayers.get(key));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public void save(){
        Gson json = new Gson();
        File dataDir = new File(NewBedwars.plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            boolean correct = dataDir.mkdirs();
            if (!correct) {
                System.out.println("[NewBedwars]: Could not create data folder!");
            }
        }
        try (FileWriter file = new FileWriter(new File(dataDir, "playersData.json"))) {
            json.toJson(NewBedwars.plugin.getPlayerManager().getPlayers(), file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, PlayerIns> castToHashMap(HashMap<String, LinkedTreeMap> hashMap) {
        HashMap<String, PlayerIns> newHashMap = new HashMap<>();
        for (String key : hashMap.keySet()) {
            LinkedTreeMap<String, Object> player = hashMap.get(key);
            Map<String, List<String>> fastBuyPerCat = new java.util.HashMap<>();
            Object rawFastBuy = player.get("fastBuyPerCategory");
            if (rawFastBuy instanceof com.google.gson.internal.LinkedTreeMap<?, ?> catMap) {
                for (Map.Entry<?, ?> entry : catMap.entrySet()) {
                    if (!(entry.getKey() instanceof String catId)) continue;
                    List<String> items = new java.util.ArrayList<>();
                    if (entry.getValue() instanceof List<?> list) {
                        for (Object o : list) { if (o instanceof String s) items.add(s); }
                    }
                    fastBuyPerCat.put(catId, items);
                }
            }
            newHashMap.put(key, new PlayerIns(
                    UUID.fromString((String) player.get("id")),
                    (String) player.get("name"),
                    Language.valueOf((String) player.get("language")),
                    fastBuyPerCat
            ));
        }
        return newHashMap;
    }
}
