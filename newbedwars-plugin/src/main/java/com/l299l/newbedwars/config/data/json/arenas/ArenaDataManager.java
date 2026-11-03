package com.l299l.newbedwars.config.data.json.arenas;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.player.LobbyService;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.PluginFiles;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.*;

public class ArenaDataManager {
    public ArenaDataManager() {
        super();
    }

    public void load() {
        File arenasDir = new File(NewBedwars.plugin.getDataFolder(), "data/arenas");
        if (arenasDir.exists()) {
            File[] arenas = arenasDir.listFiles();
            if (arenas != null) {
                for (File arena: arenas) {
                    if (!arena.getName().endsWith(".json")) continue;
                    new ArenaDataJson(PluginFiles.readFileContent(arena));
                }
            }
        }
    }

    public IArena reloadArena(String name) {
        unloadArena(name);
        return loadArena(name);
    }

    public String unloadArena(String name) {
        IArena old = ArenaRegistry.byName(name);
        if (old == null) return null;
        World world = old.getArenaWorld();
        String worldName = world.getName();
        for (org.bukkit.entity.Player p : new java.util.ArrayList<>(world.getPlayers())) {
            LobbyService.sendToLobby(p, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.ARENA_UNLOADED);
        }
        world.setAutoSave(false);
        if (!Bukkit.unloadWorld(world, false)) return null;
        ArenaRegistry.unregister(name, world);
        return worldName;
    }

    public IArena loadArena(String name) {
        File file = new File(NewBedwars.plugin.getDataFolder(), "data/arenas/" + name + ".json");
        if (!file.isFile()) return null;
        new ArenaDataJson(PluginFiles.readFileContent(file));
        return ArenaRegistry.byName(name);
    }

    public void save() {
        for (IArena arena : ArenaRegistry.all()) {
            ArenaDataJson arenaDataJson = new ArenaDataJson(arena);
            arenaDataJson.save();
        }
    }
}
