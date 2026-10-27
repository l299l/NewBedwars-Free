package com.l299l.newbedwars.world.template;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.world.chunkgenerators.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.List;

/**
 * Resets an arena world from its template: unload (without saving) → delete + copy template →
 * create the world again. The arena stays {@code restarting} until the new world is bound.
 * Always ends in {@link Arena#finishReset} or {@link Arena#failReset}.
 */
public final class ArenaResetPipeline {
    private static final int MAX_UNLOAD_ATTEMPTS = 50;

    private ArenaResetPipeline() {}

    public static boolean start(Arena arena) {
        World world = arena.getArenaWorld();
        if (world == null) {
            arena.failReset("the arena world is not loaded");
            return false;
        }
        world.setAutoSave(false);
        Path folder = world.getWorldFolder().toPath();
        tryUnload(arena, world, folder, 1);
        return true;
    }

    private static void tryUnload(Arena arena, World world, Path folder, int attempt) {
        // Nobody may stay in the world, otherwise it can not be unloaded.
        for (Player player : world.getPlayers()) {
            player.teleport(NewBedwars.plugin.getLobbyLocation());
        }
        // Chunks are NOT saved (save = false): game damage must never reach the disk.
        if (Bukkit.unloadWorld(world, false)) {
            arena.unbindWorld();
            restoreAndReload(arena, world.getName(), folder);
            return;
        }
        if (attempt >= MAX_UNLOAD_ATTEMPTS) {
            arena.failReset("the arena world could not be unloaded after " + attempt + " attempts");
            return;
        }
        schedule(arena, () -> tryUnload(arena, world, folder, attempt + 1), Properties.RollbackUnloadRetryTicks);
    }

    private static void restoreAndReload(Arena arena, String worldName, Path folder) {
        String name = arena.getArenaName();
        Path mapsRoot = ArenaMaps.mapsRoot();
        List<String> excludes = List.copyOf(Properties.RollbackTemplateExcludes);
        Runnable copy = () -> {
            Exception error = null;
            try {
                MapTemplateManager.restoreTemplate(mapsRoot, name, folder, excludes);
            } catch (Exception e) {
                error = e;
            }
            Exception result = error;
            if (Properties.RollbackAsyncCopy) {
                schedule(arena, () -> reload(arena, worldName, result), 0);
            } else {
                reload(arena, worldName, result);
            }
        };
        if (Properties.RollbackAsyncCopy) {
            try {
                Bukkit.getScheduler().runTaskAsynchronously(NewBedwars.plugin, copy);
            } catch (IllegalStateException | org.bukkit.plugin.IllegalPluginAccessException e) {
                // plugin is disabling; the dirty marker makes the next start restore the map
            }
        } else {
            copy.run();
        }
    }

    private static void reload(Arena arena, String worldName, Exception error) {
        if (error != null) {
            NewBedwars.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Map restore failed for " + arena.getArenaName(), error);
            arena.failReset("the map template is missing or corrupt (" + error.getMessage() + ")");
            return;
        }
        WorldCreator wc = new WorldCreator(worldName);
        wc.generator(new VoidGenerator());
        World fresh = Bukkit.createWorld(wc);
        if (fresh == null) {
            arena.failReset("the arena world could not be recreated");
            return;
        }
        ArenaMaps.applyWorldRules(fresh);
        ArenaMaps.clearDirty(arena.getArenaName());
        arena.finishReset(fresh);
    }

    private static void schedule(Arena arena, Runnable task, long delayTicks) {
        try {
            Bukkit.getScheduler().runTaskLater(NewBedwars.plugin, task, delayTicks);
        } catch (IllegalStateException | org.bukkit.plugin.IllegalPluginAccessException e) {
            // plugin is disabling; the dirty marker makes the next start restore the map
        }
    }
}
