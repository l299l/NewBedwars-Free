package com.l299l.newbedwars.config.data.json.arenas;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Files;

import java.io.*;
import java.util.HashMap;

public class ArenaDataManager {
    public ArenaDataManager() {
        super();
    }

    public void load() {
        File arenasDir = new File("plugins/NewBedwars/data/arenas/");
        if (arenasDir.exists()) {
            File[] arenas = arenasDir.listFiles();
            if (arenas != null) {
                for (File arena: arenas) {
                    new ArenaDataJson(Files.readFileContent(arena));
                }
            }
        }
    }

    public void save() {
        HashMap<String, IArena> arenas = Arena.arenaByName;
        for (String key : arenas.keySet()) {
            IArena arena = arenas.get(key);
            ArenaDataJson arenaDataJson = new ArenaDataJson(arena);
            arenaDataJson.save();
        }
    }
}
