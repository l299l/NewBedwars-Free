package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.maintenance.ArenaBackup;
import com.l299l.newbedwars.api.maintenance.ArenaMaintenance;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService.Outcome;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ApiMaintenance implements ArenaMaintenance {

    private interface Action<T> {
        CompletableFuture<T> start() throws Exception;
    }

    @Override
    public CompletableFuture<MaintenanceResult> enable(String arena) {
        return run(() -> ArenaMaintenanceService.enable(arena));
    }

    @Override
    public CompletableFuture<MaintenanceResult> disable(String arena) {
        return run(() -> ArenaMaintenanceService.disable(arena));
    }

    @Override
    public CompletableFuture<MaintenanceResult> backup(String arena) {
        return run(() -> ArenaMaintenanceService.backup(arena));
    }

    @Override
    public CompletableFuture<List<ArenaBackup>> listBackups(String arena) {
        return onMain(() -> {
            IArena found = ArenaRegistry.byName(arena);
            if (found == null) throw new NoSuchElementException("Arena " + arena + " does not exist");
            List<ArenaBackup> result = new ArrayList<>();
            for (ArenaMaintenanceService.Backup b : ArenaMaintenanceService.backups(found.getArenaName())) {
                result.add(new ArenaBackup(b.id(), b.hasArenaFile(), b.hasMap()));
            }
            return CompletableFuture.completedFuture(List.copyOf(result));
        });
    }

    @Override
    public CompletableFuture<MaintenanceResult> restore(String arena, String backupId) {
        return run(() -> ArenaMaintenanceService.restore(arena, backupId));
    }

    @Override
    public CompletableFuture<MaintenanceResult> export(String arena) {
        return run(() -> ArenaMaintenanceService.export(arena));
    }

    @Override
    public CompletableFuture<MaintenanceResult> importArena(String fileName) {
        return run(() -> ArenaMaintenanceService.importArena(fileName));
    }

    @Override
    public CompletableFuture<MaintenanceResult> copy(String arena, String newName) {
        return run(() -> ArenaMaintenanceService.copy(arena, newName));
    }

    @Override
    public CompletableFuture<MaintenanceResult> resetMap(String arena) {
        return run(() -> ArenaMaintenanceService.resetMap(arena));
    }

    @Override
    public CompletableFuture<MaintenanceResult> delete(String arena) {
        return run(() -> ArenaMaintenanceService.delete(arena));
    }

    @Override
    public Path getExportFolder() {
        return ArenaMaintenanceService.exportsDir();
    }

    static MaintenanceResult toResult(Outcome outcome) {
        String text = NewBedwars.plugin.getMessages().getMsgToConsole(outcome.key());
        for (Map.Entry<String, String> e : outcome.replacements().entrySet()) text = text.replace(e.getKey(), e.getValue());
        return new MaintenanceResult(outcome.code(), outcome.arena(), ChatColor.stripColor(text), outcome.detail());
    }

    private static CompletableFuture<MaintenanceResult> run(Supplier<CompletableFuture<Outcome>> action) {
        return onMain(() -> action.get().thenApply(ApiMaintenance::toResult));
    }

    private static <T> CompletableFuture<T> onMain(Action<T> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Runnable task = () -> {
            try {
                action.start().whenComplete((value, error) -> {
                    if (error != null) result.completeExceptionally(error);
                    else result.complete(value);
                });
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            try {
                Bukkit.getScheduler().runTask(NewBedwars.plugin, task);
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        }
        return result;
    }
}
