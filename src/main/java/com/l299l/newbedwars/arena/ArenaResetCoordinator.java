package com.l299l.newbedwars.arena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.ArenaResetEvent;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import com.l299l.newbedwars.world.chunkgenerators.VoidGenerator;
import com.l299l.newbedwars.world.template.ArenaMaps;
import com.l299l.newbedwars.world.template.ArenaResetPipeline;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;

/** Puts an arena's map back after a game: the template pipeline, its retries, or the legacy world reload. */
class ArenaResetCoordinator {
    private static final int MAX_RESET_RETRIES = 6;
    private static final long RESET_RETRY_TICKS = 200L;

    private final Arena arena;
    private int resetRetries = 0;

    ArenaResetCoordinator(Arena arena) {
        this.arena = arena;
    }

    /** See {@link Arena#rollback()}. */
    boolean rollback() {
        ApiEvents.arenaReset(arena, ArenaResetEvent.Stage.PRE);
        arena.clearPlacedBlocks();
        arena.enderDragonEnd();
        if (ArenaMaps.isEnabled()) {
            if (ArenaMaps.hasTemplate(arena.getArenaName())) {
                return ArenaResetPipeline.start(arena);
            }
            NewBedwars.plugin.getLogger().warning("Arena " + arena.getArenaName() + " has no map template; falling back to the legacy reload. "
                    + "Use /bw save in setup mode to create one.");
            boolean ok = legacyRollback();
            arena.setStatus(GameStatus.waiting);
            ApiEvents.arenaReset(arena, ArenaResetEvent.Stage.POST);
            return ok;
        }
        boolean ok = legacyRollback();
        ApiEvents.arenaReset(arena, ArenaResetEvent.Stage.POST);
        return ok;
    }

    void finishReset(World newWorld) {
        resetRetries = 0;
        arena.rebindWorld(newWorld);
        DecoUtils.removeAllArmorStands(arena);
        // a disabled arena keeps the "restarting" status that blocks joins
        arena.setStatus(arena.isEnabled() ? GameStatus.waiting : GameStatus.restarting);
        ApiEvents.arenaReset(arena, ArenaResetEvent.Stage.POST);
    }

    boolean resetMap() {
        if (!arena.getPlayers().isEmpty() || !arena.getSpectators().isEmpty()
                || (arena.status() != GameStatus.waiting && arena.isEnabled())) {
            return false;
        }
        arena.setStatus(GameStatus.restarting);
        boolean started = rollback();
        if (!ArenaMaps.isEnabled() || !ArenaMaps.hasTemplate(arena.getArenaName())) {
            arena.setStatus(arena.isEnabled() ? GameStatus.waiting : GameStatus.restarting);
        }
        return started;
    }

    void failReset(String reason) {
        String name = arena.getArenaName();
        arena.setStatus(GameStatus.restarting);
        if (resetRetries < MAX_RESET_RETRIES && ArenaMaps.hasTemplate(name)) {
            resetRetries++;
            NewBedwars.plugin.getLogger().warning("Reset of arena " + name + " failed: " + reason + ". Retrying in "
                    + RESET_RETRY_TICKS / 20 + "s (" + resetRetries + "/" + MAX_RESET_RETRIES + ")");
            try {
                Bukkit.getScheduler().runTaskLater(NewBedwars.plugin, () -> ArenaResetPipeline.retry(arena), RESET_RETRY_TICKS);
                return;
            } catch (IllegalStateException | org.bukkit.plugin.IllegalPluginAccessException e) {
                // plugin is disabling; the dirty marker makes the next start restore the map
            }
        }
        NewBedwars.plugin.getLogger().severe("Arena " + name + " was disabled: " + reason);
        arena.disableBroken(reason);
    }

    private boolean legacyRollback() {
        World world = arena.getArenaWorld();
        String worldName = arena.getWorldName();
        try {
            world.setKeepSpawnInMemory(false);
            world.setAutoSave(false);
            for (Chunk chunk : world.getLoadedChunks()) {
                chunk.unload(true);
            }
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().warning("Failed to unload chunks for arena " + arena.getArenaName() + ": " + e.getMessage());
        }
        arena.unbindWorld();
        boolean correct = Bukkit.getServer().unloadWorld(world, false);
        if (!correct) {
            Bukkit.getServer().getLogger().severe("Could not unload arena world " + worldName);
            return false;
        }
        WorldCreator wc = new WorldCreator(worldName);
        wc.generator(new VoidGenerator());
        World newWorld = Bukkit.getServer().createWorld(wc);
        if (newWorld == null) {
            Bukkit.getServer().getLogger().severe("Could not recreate arena world " + worldName);
            return false;
        }
        ArenaMaps.applyWorldRules(newWorld);
        arena.rebindWorld(newWorld);
        DecoUtils.removeAllArmorStands(arena);
        return true;
    }
}
