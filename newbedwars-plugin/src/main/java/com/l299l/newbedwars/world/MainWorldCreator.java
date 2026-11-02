package com.l299l.newbedwars.world;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.version.VersionCompat;
import com.l299l.newbedwars.world.chunkgenerators.VoidGenerator;
import com.l299l.newbedwars.world.schematic.SchematicManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.logging.Level;

public class MainWorldCreator implements ArenaWorldCreator {

    @Override
    public boolean createWorld(String worldName) {
        try {
            World w = Bukkit.getWorld(worldName);
            if (w == null) {
                WorldCreator wc = new WorldCreator(worldName);
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
                VersionCompat.disableLocatorBar(w);
            }
            return true;
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not create world " + worldName, e);
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
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not create world " + worldName + " from schematic " + schematicName, e);
            return false;
        }
    }
}

