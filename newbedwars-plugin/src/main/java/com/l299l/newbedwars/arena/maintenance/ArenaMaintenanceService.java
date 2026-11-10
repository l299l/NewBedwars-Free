package com.l299l.newbedwars.arena.maintenance;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult.Code;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.JsonUtils;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataManager;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.network.NetworkManager;
import com.l299l.newbedwars.player.LobbyService;
import com.l299l.newbedwars.world.template.ArenaMaps;
import com.l299l.newbedwars.world.template.MapTemplateManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ArenaMaintenanceService {
    private static final String TEMPLATE_PREFIX = "template/";

    public record Outcome(Code code, String key, String arena, String detail) {
        public boolean success() {
            return code == Code.OK;
        }

        public Map<String, String> replacements() {
            return Map.of(
                    "/arenaname/", arena == null ? "" : arena,
                    "/info/", detail == null ? "" : detail);
        }
    }

    public record Backup(String id, boolean hasArenaFile, boolean hasMap) {}

    private ArenaMaintenanceService() {}

    private static CompletableFuture<Outcome> done(Code code, String key, String arena, String detail) {
        return CompletableFuture.completedFuture(new Outcome(code, key, arena, detail));
    }

    private static CompletableFuture<Outcome> notFound(String name) {
        return done(Code.NOT_FOUND, "ArenaNotExists", name, null);
    }

    private static CompletableFuture<Outcome> busy(String name) {
        return done(Code.BUSY, "ArenaBusy", name, null);
    }

    public static boolean isBusy(IArena arena) {
        GameStatus s = arena.status();
        return !arena.getPlayers().isEmpty() || !arena.getSpectators().isEmpty()
                || s == GameStatus.playing || s == GameStatus.starting || s == GameStatus.ending
                || (arena.isEnabled() && s != GameStatus.waiting);
    }

    private static boolean inGame(IArena arena) {
        GameStatus s = arena.status();
        return s == GameStatus.playing || s == GameStatus.starting || s == GameStatus.ending;
    }

    public static CompletableFuture<Outcome> enable(String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (arena.isEnabled()) return done(Code.ALREADY_DONE, "AlreadyEnabled", name, null);
        if (!arena.canBeEnabled()) return done(Code.NOT_READY, "ArenaCantBeEnabled", name, null);
        if (ArenaMaps.isEnabled()) {
            try {
                ArenaMaps.createTemplate(name, arena.getArenaWorld());
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().warning("Could not save map template of " + name + ": " + e.getMessage());
                return done(Code.FAILED, "MapTemplateFailed", name, e.getMessage());
            }
        }
        arena.enable();
        return done(Code.OK, "ArenaEnabled", name, null);
    }

    public static CompletableFuture<Outcome> disable(String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (!arena.isEnabled()) return done(Code.ALREADY_DONE, "AlreadyDisabled", name, null);
        if (arena.status() != GameStatus.waiting) return done(Code.BUSY, "ArenaCantBeDisabled", name, null);
        arena.disable();
        return done(Code.OK, "ArenaDisabled", name, null);
    }

    public static CompletableFuture<Outcome> backup(String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (isBusy(arena)) return busy(name);
        String id = MapTemplateManager.timestamp();
        try {
            String json = JsonUtils.beautifyJson(arena.toJson());
            Path dir = ArenaMaps.arenaBackupDir(name);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(id + ".json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return done(Code.FAILED, "BackupFailed", name, e.getMessage());
        }
        if (!ArenaMaps.hasTemplate(name)) return done(Code.OK, "BackupCreated", name, id);
        Path zip = ArenaMaps.mapBackupDir(name).resolve(id + ".zip");
        return runAsync(() -> MapTemplateManager.zipTemplate(ArenaMaps.mapsRoot(), name, zip), error -> error == null
                ? new Outcome(Code.OK, "BackupCreated", name, id)
                : new Outcome(Code.FAILED, "BackupFailed", name, error.getMessage()));
    }

    public static List<Backup> backups(String name) throws IOException {
        List<Backup> result = new ArrayList<>();
        for (String id : backupIds(name)) {
            result.add(new Backup(id,
                    Files.isRegularFile(ArenaMaps.arenaBackupDir(name).resolve(id + ".json")),
                    Files.isRegularFile(ArenaMaps.mapBackupDir(name).resolve(id + ".zip"))));
        }
        return result;
    }

    public static Set<String> backupIds(String name) throws IOException {
        TreeSet<String> ids = new TreeSet<>(Comparator.reverseOrder());
        for (Path p : MapTemplateManager.listBackups(ArenaMaps.arenaBackupDir(name), ".json")) ids.add(baseName(p));
        for (Path p : MapTemplateManager.listBackups(ArenaMaps.mapBackupDir(name), ".zip")) ids.add(baseName(p));
        return ids;
    }

    public static CompletableFuture<Outcome> restore(String arenaName, String requestedId) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (isBusy(arena)) return busy(name);
        try {
            Path json = pickBackup(ArenaMaps.arenaBackupDir(name), requestedId, ".json");
            Path zip = pickBackup(ArenaMaps.mapBackupDir(name), requestedId, ".zip");
            if (json == null && zip == null) return done(Code.NOT_FOUND, "RestoreNothing", name, requestedId);
            ArenaDataManager manager = NewBedwars.plugin.getDataManager().getArenaDataManager();
            IOExecutor.flush();
            String worldName = manager.unloadArena(name);
            if (worldName == null) return done(Code.FAILED, "RestoreFailed", name, "the arena world could not be unloaded");
            if (json != null) {
                AtomicFileWriter.write(arenaFile(name), Files.readString(json, StandardCharsets.UTF_8),
                        ArenaMaps.arenaBackupDir(name), Math.max(1, Properties.BackupsKeep));
            }
            if (zip != null) MapTemplateManager.restoreTemplateFromZip(ArenaMaps.mapsRoot(), name, zip);
            if (ArenaMaps.hasTemplate(name)) ArenaMaps.restoreOffline(name, worldName);
            IArena loaded = manager.loadArena(name);
            return loaded != null
                    ? done(Code.OK, "RestoreDone", name, null)
                    : done(Code.FAILED, "RestoreFailed", name, "the restored arena could not be loaded");
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Restore of arena " + name + " failed", e);
            if (NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(name) == null) {
                NewBedwars.plugin.getLogger().severe("Arena " + name + " could not be loaded again after the failed restore");
            }
            return done(Code.FAILED, "RestoreFailed", name, e.getMessage());
        }
    }

    private static Path pickBackup(Path dir, String id, String extension) throws IOException {
        if (id == null) {
            List<Path> all = MapTemplateManager.listBackups(dir, extension);
            return all.isEmpty() ? null : all.get(0);
        }
        if (!ArenaTransfer.isSafeName(id)) return null;
        Path file = dir.resolve(id + extension);
        return Files.isRegularFile(file) ? file : null;
    }

    public static CompletableFuture<Outcome> resetMap(String arenaName) {
        IArena iArena = ArenaRegistry.byName(arenaName);
        if (iArena == null) return notFound(arenaName);
        String name = iArena.getArenaName();
        if (!(iArena instanceof Arena arena) || isBusy(iArena) || !arena.resetMap()) return busy(name);
        return done(Code.OK, "ResetMapStarted", name, null);
    }

    public static CompletableFuture<Outcome> export(String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (isBusy(arena)) return busy(name);
        String json;
        try {
            json = JsonUtils.beautifyJson(arena.toJson());
        } catch (IOException e) {
            return done(Code.FAILED, "ExportFailed", name, e.getMessage());
        }
        String fileName = name + "-" + MapTemplateManager.timestamp() + ".zip";
        Path target = exportsDir().resolve(fileName);
        return runAsync(() -> {
            Files.createDirectories(target.getParent());
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
                zip.putNextEntry(new ZipEntry(ArenaTransfer.ARENA_ENTRY));
                zip.write(json.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                if (ArenaMaps.hasTemplate(name)) {
                    MapTemplateManager.addTree(zip, MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), name), TEMPLATE_PREFIX);
                }
            }
        }, error -> error == null
                ? new Outcome(Code.OK, "ExportDone", name, "data/exports/" + fileName)
                : new Outcome(Code.FAILED, "ExportFailed", name, error.getMessage()));
    }

    public static CompletableFuture<Outcome> copy(String arenaName, String newName) {
        if (NetworkManager.isLobby()) return done(Code.UNAVAILABLE, "MaintenanceOnLobby", newName, null);
        IArena source = ArenaRegistry.byName(arenaName);
        if (source == null) return notFound(arenaName);
        String name = source.getArenaName();
        if (!ArenaTransfer.isSafeName(newName)) {
            return done(Code.INVALID_NAME, "CopyFailed", newName, "use only letters, digits, _ and -");
        }
        Path arenaFile = arenaFile(newName);
        if (ArenaRegistry.find(newName) != null || Files.exists(arenaFile)
                || Bukkit.getWorld(newName) != null || Files.exists(ArenaMaps.worldFolder(newName))
                || MapTemplateManager.hasTemplate(ArenaMaps.mapsRoot(), newName)) {
            return done(Code.NAME_TAKEN, "CopyFailed", newName, "an arena, map or world with that name already exists");
        }
        Path mapSource;
        if (ArenaMaps.hasTemplate(name)) {
            mapSource = MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), name);
        } else {
            if (isBusy(source)) return busy(name);
            source.getArenaWorld().save();
            mapSource = source.getArenaWorld().getWorldFolder().toPath();
        }
        String json;
        try {
            JsonObject obj = new Gson().fromJson(source.toJson(), JsonObject.class);
            String oldWorld = obj.get("worldName").getAsString();
            obj.addProperty("arenaName", newName);
            obj.addProperty("worldName", newName);
            JsonUtils.renameWorld(obj, oldWorld, newName);
            json = JsonUtils.beautifyJson(obj.toString());
        } catch (Exception e) {
            return done(Code.FAILED, "CopyFailed", newName, e.getMessage());
        }
        List<String> excludes = List.copyOf(Properties.RollbackTemplateExcludes);
        return runAsync(() -> {
            MapTemplateManager.createTemplate(ArenaMaps.mapsRoot(), newName, mapSource, excludes);
            ArenaTransfer.stripWorldIdentity(MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), newName));
            AtomicFileWriter.write(arenaFile, json, null, 0);
        }, error -> finishCopy(name, newName, arenaFile, error));
    }

    private static Outcome finishCopy(String name, String newName, Path arenaFile, Exception error) {
        if (error != null) {
            NewBedwars.plugin.getLogger().warning("Copy of arena " + name + " to " + newName + " failed: " + error);
            removeFiles(newName, newName, arenaFile);
            return new Outcome(Code.FAILED, "CopyFailed", newName, error.getMessage());
        }
        IArena loaded = null;
        try {
            ArenaMaps.restoreOffline(newName, newName);
            loaded = NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(newName);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().warning("Copy of arena " + name + " to " + newName + " failed: " + e);
        }
        if (loaded == null) {
            removeFiles(newName, newName, arenaFile);
            return new Outcome(Code.FAILED, "CopyFailed", newName, "the copy could not be loaded (see the console)");
        }
        ArenaGroupManager groups = ArenaGroupManager.get();
        ArenaGroup group = groups == null ? null : groups.groupOf(name);
        if (group != null) {
            group.add(newName);
            groups.changed();
        }
        return new Outcome(Code.OK, "CopyDone", newName, name + (group != null ? ", group " + group.getName() : ""));
    }

    public static CompletableFuture<Outcome> importArena(String fileName) {
        if (NetworkManager.isLobby()) return done(Code.UNAVAILABLE, "MaintenanceOnLobby", fileName, null);
        Path zip = ArenaTransfer.resolveExport(exportsDir(), fileName);
        if (zip == null) return done(Code.NOT_FOUND, "ImportFailed", fileName, "file not found in data/exports");
        Path tmp = exportsDir().resolve(".import-" + MapTemplateManager.timestamp());
        String createdName = null;
        String createdWorld = null;
        try {
            MapTemplateManager.unzip(zip, tmp);
            ArenaTransfer.Imported imported = ArenaTransfer.readImport(tmp);
            String name = imported.name();
            String worldName = imported.worldName();
            Path arenaFile = arenaFile(name);
            if (ArenaRegistry.contains(name) || Files.exists(arenaFile) || Files.exists(ArenaMaps.worldFolder(worldName))
                    || Bukkit.getWorld(worldName) != null || ArenaMaps.hasTemplate(name)) {
                return done(Code.NAME_TAKEN, "ImportFailed", fileName,
                        "an arena or world named '" + name + "' / '" + worldName + "' already exists");
            }
            createdName = name;
            createdWorld = worldName;
            Path mapTemplate = MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), name);
            Files.createDirectories(mapTemplate.getParent());
            MapTemplateManager.copyTree(imported.template(), mapTemplate, List.of());
            ArenaTransfer.stripWorldIdentity(mapTemplate);
            AtomicFileWriter.write(arenaFile, JsonUtils.beautifyJson(imported.json().toString()), null, 0);
            ArenaMaps.restoreOffline(name, worldName);
            IArena loaded = NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(name);
            if (loaded == null) {
                removeFiles(name, worldName, arenaFile);
                return done(Code.FAILED, "ImportFailed", name, "the imported arena could not be loaded");
            }
            return done(Code.OK, "ImportDone", name, null);
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Import of " + fileName + " failed", e);
            if (createdName != null) removeFiles(createdName, createdWorld, arenaFile(createdName));
            return done(Code.FAILED, "ImportFailed", fileName, e.getMessage());
        } finally {
            try {
                MapTemplateManager.deleteTree(tmp);
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not delete " + tmp, e);
            }
        }
    }

    public static CompletableFuture<Outcome> delete(String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (inGame(arena)) return done(Code.BUSY, "ArenaCannotBeDeleted", name, null);
        World arenaWorld = arena.getArenaWorld();
        if (arenaWorld.equals(Bukkit.getWorlds().get(0))) return done(Code.FAILED, "ArenaDeleteFailed", name, null);
        Path worldFolder = arenaWorld.getWorldFolder().toPath();

        DecoUtils.removeAllArmorStands(arena);
        Location lobby = NewBedwars.plugin.getLobbyLocation();
        for (Player p : new ArrayList<>(arenaWorld.getPlayers())) {
            p.setInvulnerable(false);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            if (!LobbyService.sendToLobby(p, PlayerSendToLobbyEvent.Reason.ARENA_DELETED) && p.getWorld().equals(arenaWorld)) {
                p.teleport(lobby);
            }
        }

        arenaWorld.setAutoSave(false);
        if (!Bukkit.unloadWorld(arenaWorld, false)) return done(Code.FAILED, "ArenaDeleteFailed", name, null);
        ArenaRegistry.unregister(name, arenaWorld);

        IOExecutor.flush();
        boolean complete = deleteQuietly(worldFolder);
        complete &= deleteQuietly(arenaFile(name));
        complete &= deleteQuietly(ArenaMaps.mapsRoot().resolve(name));
        return complete
                ? done(Code.OK, "ArenaDeleted", name, null)
                : done(Code.PARTIAL, "ArenaDeletedPartially", name, null);
    }

    private static boolean deleteQuietly(Path path) {
        try {
            MapTemplateManager.deleteTree(path);
            return true;
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not delete " + path, e);
            return false;
        }
    }

    private static void removeFiles(String name, String worldName, Path arenaFile) {
        try {
            World world = Bukkit.getWorld(worldName);
            if (world != null) Bukkit.unloadWorld(world, false);
            Files.deleteIfExists(arenaFile);
            MapTemplateManager.deleteTree(MapTemplateManager.arenaRoot(ArenaMaps.mapsRoot(), name));
            MapTemplateManager.deleteTree(ArenaMaps.worldFolder(worldName));
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not clean up the failed arena " + name, e);
        }
    }

    public static Path exportsDir() {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("exports").toAbsolutePath().normalize();
    }

    public static List<String> exportFiles() {
        Path dir = exportsDir();
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".zip")).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Path arenaFile(String name) {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("arenas").resolve(name + ".json");
    }

    private static String baseName(Path p) {
        String n = p.getFileName().toString();
        return n.substring(0, n.lastIndexOf('.'));
    }

    private interface IoTask {
        void run() throws Exception;
    }

    private interface Finish {
        Outcome apply(Exception error);
    }

    private static CompletableFuture<Outcome> runAsync(IoTask task, Finish finish) {
        CompletableFuture<Outcome> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(NewBedwars.plugin, () -> {
            Exception error = null;
            try {
                task.run();
            } catch (Exception e) {
                error = e;
            }
            Exception result = error;
            Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
                try {
                    future.complete(finish.apply(result));
                } catch (RuntimeException e) {
                    future.completeExceptionally(e);
                }
            });
        });
        return future;
    }
}
