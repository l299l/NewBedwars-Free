package com.l299l.newbedwars.api.maintenance;

import com.l299l.newbedwars.api.NewBedwarsAPI;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The arena actions of {@code /bw arena} (enable, disable, backups, export/import, copy, map reset, delete) for
 * addons such as a web panel. Get it with {@link NewBedwarsAPI#getMaintenance()}.
 * <p>
 * The methods can be called from any thread; the work runs on the server thread (file copying in the
 * background) and the futures complete on the server thread. The same checks as the commands apply, so a
 * refused action completes normally with a {@link MaintenanceResult} that is not a success; a future only
 * completes exceptionally on an unexpected error.
 */
public interface ArenaMaintenance {

    CompletableFuture<MaintenanceResult> enable(String arena);

    /** Players waiting in the arena are sent out. */
    CompletableFuture<MaintenanceResult> disable(String arena);

    /** Saves the arena file and its map; {@link MaintenanceResult#detail()} is the backup id. */
    CompletableFuture<MaintenanceResult> backup(String arena);

    /**
     * The backups of an arena, newest first. Completes exceptionally with {@link java.util.NoSuchElementException}
     * when the arena does not exist and with {@link java.io.IOException} when the backups can not be read.
     */
    CompletableFuture<List<ArenaBackup>> listBackups(String arena);

    /** Restores the arena file and map from a backup; {@code backupId} null = the newest one. */
    CompletableFuture<MaintenanceResult> restore(String arena, String backupId);

    /** Writes a zip (arena file + map) into {@link #getExportFolder()}; {@link MaintenanceResult#detail()} is its path. */
    CompletableFuture<MaintenanceResult> export(String arena);

    /** Loads an arena from a zip in {@link #getExportFolder()}. The new arena is disabled. */
    CompletableFuture<MaintenanceResult> importArena(String fileName);

    /** Copies settings and map to a new arena, which joins the same group. */
    CompletableFuture<MaintenanceResult> copy(String arena, String newName);

    /** Puts the map back to its saved state; completes when the reset was started. */
    CompletableFuture<MaintenanceResult> resetMap(String arena);

    /** Permanently deletes the arena file, its world and its saved map; backups in {@code data/backups} are kept. */
    CompletableFuture<MaintenanceResult> delete(String arena);

    /** {@code plugins/NewBedwars/data/exports}: where exports are written and imports are read from. */
    Path getExportFolder();
}
