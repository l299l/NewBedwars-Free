package com.l299l.newbedwars.world.template;

import com.l299l.newbedwars.player.LobbyService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ArenaResetPipeline {
    private static final int MAX_UNLOAD_ATTEMPTS = 50;
    private static final ConcurrentHashMap<String, CountDownLatch> COPYING = new ConcurrentHashMap<>();

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

    public static void retry(Arena arena) {
        World world = arena.getArenaWorld();
        if (world == null) {
            arena.failReset("the arena world is not known");
            return;
        }
        if (Bukkit.getWorld(world.getName()) != null) {
            start(arena);
            return;
        }
        arena.unbindWorld();
        restoreAndReload(arena, world.getName(), ArenaMaps.worldFolder(world.getName()));
    }

    private static void tryUnload(Arena arena, World world, Path folder, int attempt) {
        for (Player player : world.getPlayers()) {
            LobbyService.sendToLobby(player, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.MAP_RESET);
        }
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
        CountDownLatch done = new CountDownLatch(1);
        Runnable copy = () -> {
            Exception error = null;
            try {
                MapTemplateManager.restoreTemplate(mapsRoot, name, folder, excludes);
            } catch (Exception e) {
                error = e;
            } catch (Throwable t) {
                error = new java.io.IOException(t.toString(), t);
            } finally {
                COPYING.remove(name, done);
                done.countDown();
            }
            Exception result = error;
            if (Properties.RollbackAsyncCopy) {
                schedule(arena, () -> reload(arena, worldName, result), 0);
            } else {
                reload(arena, worldName, result);
            }
        };
        if (Properties.RollbackAsyncCopy) {
            COPYING.put(name, done);
            try {
                Bukkit.getScheduler().runTaskAsynchronously(NewBedwars.plugin, copy);
            } catch (IllegalStateException | org.bukkit.plugin.IllegalPluginAccessException e) {
                COPYING.remove(name, done);
                NewBedwars.plugin.getLogger().warning("Map reset of arena " + name
                        + " could not start because the plugin is stopping; the map is restored on shutdown or the next start");
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

    public static void awaitRunningCopies(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        for (java.util.Map.Entry<String, CountDownLatch> e : COPYING.entrySet()) {
            long left = deadline - System.currentTimeMillis();
            try {
                if (left <= 0 || !e.getValue().await(left, TimeUnit.MILLISECONDS)) {
                    NewBedwars.plugin.getLogger().warning("Map reset of arena " + e.getKey() + " is still running at shutdown");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static boolean anyCopying() {
        return !COPYING.isEmpty();
    }

    public static String copyingArena() {
        return COPYING.keySet().stream().findFirst().orElse(null);
    }

    public static boolean isCopying(String arena) {
        return COPYING.containsKey(arena);
    }

    private static void schedule(Arena arena, Runnable task, long delayTicks) {
        try {
            Bukkit.getScheduler().runTaskLater(NewBedwars.plugin, task, delayTicks);
        } catch (IllegalStateException | org.bukkit.plugin.IllegalPluginAccessException e) {
            NewBedwars.plugin.getLogger().warning("Map reset of arena " + arena.getArenaName()
                    + " stopped because the plugin is stopping; the map is restored on shutdown or the next start");
        }
    }
}
