package com.l299l.newbedwars.events;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class InitializationEvent implements Listener {
    private final NewBedwars plugin;

    public InitializationEvent(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerStart(ServerLoadEvent event) {
        plugin.getLogger().info("Loading arenas data...");
        plugin.getDataManager().loadArenas();
        plugin.getLogger().info("Arenas data loaded!");
    }
}
