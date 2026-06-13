package com.l299l.newbedwars.config.data;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LobbyData {

    private static File file() {
        return new File(NewBedwars.plugin.getDataFolder(), "data/lobby.yml");
    }

    public static void save(Location location) {
        if (location == null) return;
        File f = file();
        f.getParentFile().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("world", location.getWorld().getName());
        yaml.set("x", location.getX());
        yaml.set("y", location.getY());
        yaml.set("z", location.getZ());
        yaml.set("yaw", (double) location.getYaw());
        yaml.set("pitch", (double) location.getPitch());
        try {
            yaml.save(f);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().severe("Could not save lobby location: " + e.getMessage());
        }
    }

    public static Location load() {
        File f = file();
        if (!f.exists()) return null;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
        String worldName = yaml.getString("world");
        if (worldName == null || worldName.isEmpty()) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                yaml.getDouble("x"),
                yaml.getDouble("y"),
                yaml.getDouble("z"),
                (float) yaml.getDouble("yaw"),
                (float) yaml.getDouble("pitch"));
    }
}
