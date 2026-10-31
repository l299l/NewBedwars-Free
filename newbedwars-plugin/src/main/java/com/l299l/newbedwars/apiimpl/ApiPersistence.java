package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;

import java.util.HashSet;
import java.util.Set;

/** Saves arena files after API changes, collecting bursts of changes into one write. */
public final class ApiPersistence {
    private static final long DELAY_TICKS = 100L;
    private static final Set<String> PENDING = new HashSet<>();

    private ApiPersistence() {}

    public static void requestSave(IArena arena) {
        String name = arena.getArenaName();
        if (!PENDING.add(name)) return;
        try {
            Bukkit.getScheduler().runTaskLater(NewBedwars.plugin, () -> {
                PENDING.remove(name);
                if (ArenaRegistry.byName(name) == arena) saveNow(arena);
            }, DELAY_TICKS);
        } catch (IllegalStateException | IllegalPluginAccessException e) {
            PENDING.remove(name);
        }
    }

    public static void saveNow(IArena arena) {
        new ArenaDataJson(arena).save();
    }
}
