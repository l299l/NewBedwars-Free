package com.l299l.newbedwars.world;

import com.l299l.newbedwars.world.chunkgenerators.VoidGenerator;
import com.l299l.newbedwars.world.schematic.SchematicManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

public class MainWorldCreator implements WorldCreator {

    @Override
    public boolean createWorld(String worldName) {
        try {
            World w = Bukkit.getWorld(worldName);
            if (w == null) {
                org.bukkit.WorldCreator wc = new org.bukkit.WorldCreator(worldName);
                wc.generator(new VoidGenerator());
                w = wc.createWorld();
                assert w != null;
                w.setSpawnLocation(0, 64, 0);
                w.setGameRule(GameRule.KEEP_INVENTORY, true);
                w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                w.setGameRule(GameRule.DO_FIRE_TICK, false);
                w.setGameRule(GameRule.DISABLE_RAIDS, false);
                w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
                w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
                w.setGameRule(GameRule.DO_INSOMNIA, false);
                w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            }
            Bukkit.getWorlds().add(w);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean createWorldFromSchematic(String worldName, String schematicName) {
        try {
            if (!createWorld(worldName)) return false;
            World w = Bukkit.getWorld(worldName);
            if (w == null) return false;
            SchematicManager.pasteIntoWorld(w, schematicName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

