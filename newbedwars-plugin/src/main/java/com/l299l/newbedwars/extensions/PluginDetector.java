package com.l299l.newbedwars.extensions;

import org.bukkit.Bukkit;

public class PluginDetector {
    public static boolean isWorldEditPresent() {
        return Bukkit.getPluginManager().getPlugin("WorldEdit") != null
                || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }
    public static boolean isPlaceholderAPIPresent() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static boolean isCitizensPresent() {
        return Bukkit.getPluginManager().getPlugin("Citizens") != null &&
                Bukkit.getPluginManager().isPluginEnabled("Citizens");
    }
}
