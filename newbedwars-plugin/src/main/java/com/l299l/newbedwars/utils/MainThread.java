package com.l299l.newbedwars.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

public final class MainThread {
    private MainThread() {
    }

    public static boolean run(Plugin plugin, Runnable task) {
        if (plugin == null || !plugin.isEnabled()) return false;
        try {
            Bukkit.getScheduler().runTask(plugin, task);
            return true;
        } catch (IllegalPluginAccessException | IllegalStateException e) {
            return false;
        }
    }
}
