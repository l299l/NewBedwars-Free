package com.l299l.newbedwars.world.template;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ArenaMaps {
    private ArenaMaps() {}

    public static boolean isEnabled() {
        return !Properties.RollbackLegacySystem;
    }

    public static Path mapsRoot() {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("maps");
    }

    public static Path mapBackupDir(String arena) {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("backups").resolve("maps").resolve(arena);
    }

    public static Path arenaBackupDir(String arena) {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("backups").resolve("arenas").resolve(arena);
    }

    public static Path worldFolder(String worldName) {
        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) return loaded.getWorldFolder().toPath();
        Path container = Bukkit.getWorldContainer().toPath();
        Path classic = container.resolve(worldName);
        if (Files.exists(classic) || Bukkit.getWorlds().isEmpty()) return classic;
        Path dimension = container.resolve(Bukkit.getWorlds().get(0).getName()).resolve("dimensions").resolve("minecraft")
                .resolve(worldName.toLowerCase(java.util.Locale.ROOT));
        return Files.isDirectory(dimension.getParent()) ? dimension : classic;
    }

    public static boolean hasTemplate(String arena) {
        return MapTemplateManager.hasTemplate(mapsRoot(), arena);
    }

    public static boolean isDirty(String arena) {
        return MapTemplateManager.isDirty(mapsRoot(), arena);
    }

    public static void markDirty(String arena) {
        try {
            MapTemplateManager.markDirty(mapsRoot(), arena);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not write dirty marker for arena " + arena, e);
        }
    }

    public static void clearDirty(String arena) {
        try {
            MapTemplateManager.clearDirty(mapsRoot(), arena);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not clear dirty marker for arena " + arena, e);
        }
    }

    public static void createTemplate(String arena, World world) throws IOException {
        world.save();
        backupTemplate(arena);
        MapTemplateManager.createTemplate(mapsRoot(), arena, world.getWorldFolder().toPath(), Properties.RollbackTemplateExcludes);
    }

    public static void createTemplateAsync(String arena, World world, Consumer<Exception> done) {
        world.save();
        Path folder = world.getWorldFolder().toPath();
        List<String> excludes = List.copyOf(Properties.RollbackTemplateExcludes);
        Bukkit.getScheduler().runTaskAsynchronously(NewBedwars.plugin, () -> {
            Exception error = null;
            try {
                backupTemplate(arena);
                MapTemplateManager.createTemplate(mapsRoot(), arena, folder, excludes);
            } catch (Exception e) {
                error = e;
            }
            Exception result = error;
            Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> done.accept(result));
        });
    }

    public static void restoreOffline(String arena, String worldName) throws IOException {
        MapTemplateManager.restoreTemplate(mapsRoot(), arena, worldFolder(worldName), Properties.RollbackTemplateExcludes);
        MapTemplateManager.clearDirty(mapsRoot(), arena);
    }

    private static void backupTemplate(String arena) throws IOException {
        int keep = Properties.BackupsKeep;
        if (keep <= 0 || !hasTemplate(arena)) return;
        MapTemplateManager.zipTemplate(mapsRoot(), arena, mapBackupDir(arena).resolve(MapTemplateManager.timestamp() + ".zip"));
        MapTemplateManager.rotate(mapBackupDir(arena), ".zip", keep);
    }

    public static void applyWorldRules(World world) {
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        VersionCompat.disableLocatorBar(world);
    }
}
