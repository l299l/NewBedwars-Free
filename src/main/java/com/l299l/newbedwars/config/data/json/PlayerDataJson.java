package com.l299l.newbedwars.config.data.json;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.player.PlayerIns;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;

public class PlayerDataJson {

    public void load(){
        File playerFile = new File("plugins/NewBedwars/data/playersData.json");
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
        File dataDir = new File("plugins/NewBedwars/data/");
        if (!dataDir.exists()) {
            boolean correct = dataDir.mkdir();
            if (!correct) {
                System.out.println("[NewBedwars]: Could not create data folder!");
            }
        }
        try (FileWriter file = new FileWriter("plugins/NewBedwars/data/playersData.json")) {
            json.toJson(NewBedwars.plugin.getPlayerManager().getPlayers(), file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, PlayerIns> castToHashMap(HashMap<String, LinkedTreeMap> hashMap) {
        HashMap<String, PlayerIns> newHashMap = new HashMap<>();
        for (String key: hashMap.keySet()) {
            LinkedTreeMap<String, Object> player = hashMap.get(key);
            newHashMap.put(key, new PlayerIns(UUID.fromString((String) player.get("id")), (String) player.get("name"), Language.valueOf((String) player.get("language")), (String) player.get("shopGui"), (String) player.get("upgradeGui")));
        }
        return newHashMap;

    }
}
