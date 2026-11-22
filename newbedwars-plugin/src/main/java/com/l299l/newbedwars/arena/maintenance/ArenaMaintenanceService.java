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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

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

    private static final Set<String> IN_FLIGHT = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static String key(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean claim(String name) {
        return IN_FLIGHT.add(key(name));
    }

    private static void release(String name) {
        IN_FLIGHT.remove(key(name));
    }

    private static final Set<String> EDIT_LOCKED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static boolean editLocked(String name) {
        return EDIT_LOCKED.contains(key(name));
    }

    public static boolean inFlight(String name) {
        return IN_FLIGHT.contains(key(name));
    }

    public static String anyInFlight() {
        return IN_FLIGHT.stream().findFirst().orElse(null);
    }

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
                || (arena.isEnabled() && s != GameStatus.waiting)
                || inFlight(arena.getArenaName());
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
        if (!ArenaMaps.isEnabled()) {
            arena.enable();
            return done(Code.OK, "ArenaEnabled", name, null);
        }
        if (com.l299l.newbedwars.world.template.ArenaResetPipeline.isCopying(name) || !claim(name)) return busy(name);
        EDIT_LOCKED.add(key(name));
        CompletableFuture<Outcome> future = new CompletableFuture<>();
        try {
            ArenaMaps.createTemplateAsync(name, arena.getArenaWorld(), error -> {
                release(name);
                EDIT_LOCKED.remove(key(name));
                if (error != null) {
                    NewBedwars.plugin.getLogger().warning("Could not save map template of " + name + ": " + error.getMessage());
                    future.complete(new Outcome(Code.FAILED, "MapTemplateFailed", name, error.getMessage()));
                    return;
                }
                if (ArenaRegistry.byName(name) != arena || arena.isEnabled()) {
                    future.complete(new Outcome(Code.FAILED, "MapTemplateFailed", name, "the arena changed while its map was saved"));
                    return;
                }
                if (!arena.canBeEnabled()) {
                    future.complete(new Outcome(Code.NOT_READY, "ArenaCantBeEnabled", name, null));
                    return;
                }
                arena.enable();
                future.complete(new Outcome(Code.OK, "ArenaEnabled", name, null));
            });
        } catch (RuntimeException e) {
            release(name);
            EDIT_LOCKED.remove(key(name));
            return done(Code.FAILED, "MapTemplateFailed", name, e.getMessage());
        }
        return future;
    }

    public static boolean saveTemplate(IArena arena, java.util.function.Consumer<Exception> done) {
        String name = arena.getArenaName();
        if (com.l299l.newbedwars.world.template.ArenaResetPipeline.isCopying(name) || !claim(name)) return false;
        EDIT_LOCKED.add(key(name));
        try {
            ArenaMaps.createTemplateAsync(name, arena.getArenaWorld(), error -> {
                release(name);
                EDIT_LOCKED.remove(key(name));
                done.accept(error);
            });
        } catch (RuntimeException e) {
            release(name);
            EDIT_LOCKED.remove(key(name));
            done.accept(e);
        }
        return true;
    }

    public static CompletableFuture<Outcome> disable(String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (!arena.isEnabled()) {
            if (arena instanceof Arena loaded && loaded.isAutoDisabled()) {
                loaded.clearAutoDisabled();
                return done(Code.OK, "ArenaDisabled", name, null);
            }
            return done(Code.ALREADY_DONE, "AlreadyDisabled", name, null);
        }
        if (arena.status() != GameStatus.waiting) return done(Code.BUSY, "ArenaCantBeDisabled", name, null);
        arena.disable();
        return done(Code.OK, "ArenaDisabled", name, null);
    }

    public static CompletableFuture<Outcome> backup(String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (isBusy(arena) || !claim(name)) return busy(name);
        String id = MapTemplateManager.timestamp() + MapTemplateManager.MANUAL_SUFFIX;
        try {
            String json = JsonUtils.beautifyJson(arena.toJson());
            Path dir = ArenaMaps.arenaBackupDir(name);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(id + ".json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            release(name);
            return done(Code.FAILED, "BackupFailed", name, e.getMessage());
        }
        if (!ArenaMaps.hasTemplate(name)) {
            release(name);
            return done(Code.OK, "BackupCreated", name, id);
        }
        Path zip = ArenaMaps.mapBackupDir(name).resolve(id + ".zip");
        return runAsync(() -> MapTemplateManager.zipTemplate(ArenaMaps.mapsRoot(), name, zip), error -> {
            release(name);
            return error == null
                    ? new Outcome(Code.OK, "BackupCreated", name, id)
                    : new Outcome(Code.FAILED, "BackupFailed", name, error.getMessage());
        });
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
        if (isBusy(arena) || com.l299l.newbedwars.world.template.ArenaResetPipeline.isCopying(name)) return busy(name);
        if (!claim(name)) return busy(name);
        try {
            String id = requestedId == null ? newestManualBackup(name) : requestedId;
            Path json = pickBackup(ArenaMaps.arenaBackupDir(name), id, ".json");
            Path zip = pickBackup(ArenaMaps.mapBackupDir(name), id, ".zip");
            if (json == null && zip == null) {
                release(name);
                return done(Code.NOT_FOUND, "RestoreNothing", name, requestedId);
            }
            ArenaDataManager manager = NewBedwars.plugin.getDataManager().getArenaDataManager();
            IOExecutor.flush();
            String worldName = manager.unloadArena(name);
            if (worldName == null) {
                release(name);
                return done(Code.FAILED, "RestoreFailed", name, "the arena world could not be unloaded");
            }
            Path restoreTarget = ArenaMaps.worldFolder(worldName);
            List<String> restoreExcludes = List.copyOf(Properties.RollbackTemplateExcludes);
            return runAsync(() -> {
                String restoredJson = json == null ? null : Files.readString(json, StandardCharsets.UTF_8);
                if (zip != null) MapTemplateManager.restoreTemplateFromZip(ArenaMaps.mapsRoot(), name, zip);
                if (restoredJson != null) {
                    AtomicFileWriter.write(arenaFile(name), restoredJson,
                            ArenaMaps.arenaBackupDir(name), Math.max(1, Properties.BackupsKeep));
                }
                if (ArenaMaps.hasTemplate(name)) {
                    ArenaMaps.markDirty(name);
                    ArenaMaps.restoreOffline(name, restoreTarget, restoreExcludes);
                }
            }, error -> {
                release(name);
                return finishRestore(manager, name, error);
            });
        } catch (Exception e) {
            release(name);
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Restore of arena " + name + " failed", e);
            if (ArenaRegistry.byName(name) == null) {
                try {
                    NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(name);
                } catch (RuntimeException again) {
                    NewBedwars.plugin.getLogger().log(Level.SEVERE, "Arena " + name + " could not be loaded again", again);
                }
            }
            return done(Code.FAILED, "RestoreFailed", name, e.getMessage());
        }
    }

    private static Outcome finishRestore(ArenaDataManager manager, String name, Exception error) {
        if (error != null) NewBedwars.plugin.getLogger().log(Level.SEVERE, "Restore of arena " + name + " failed", error);
        IArena loaded;
        try {
            loaded = manager.loadArena(name);
        } catch (RuntimeException e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Arena " + name + " could not be loaded after the restore", e);
            loaded = null;
        }
        if (error != null) {
            if (loaded == null) NewBedwars.plugin.getLogger().severe("Arena " + name + " could not be loaded again after the failed restore");
            return new Outcome(Code.FAILED, "RestoreFailed", name, error.getMessage());
        }
        return loaded != null
                ? new Outcome(Code.OK, "RestoreDone", name, null)
                : new Outcome(Code.FAILED, "RestoreFailed", name, "the restored arena could not be loaded");
    }

    private static String newestManualBackup(String name) throws IOException {
        for (String id : backupIds(name)) {
            if (id.endsWith(MapTemplateManager.MANUAL_SUFFIX)) return id;
        }
        return null;
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
        if (isBusy(arena) || !claim(name)) return busy(name);
        String json;
        try {
            json = JsonUtils.beautifyJson(arena.toJson());
        } catch (IOException e) {
            release(name);
            return done(Code.FAILED, "ExportFailed", name, e.getMessage());
        }
        String fileName = name + "-" + MapTemplateManager.timestamp() + ".zip";
        Path target = exportsDir().resolve(fileName);
        return runAsync(() -> {
            MapTemplateManager.writeZip(target, zip -> {
                zip.putNextEntry(new ZipEntry(ArenaTransfer.ARENA_ENTRY));
                zip.write(json.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                if (ArenaMaps.hasTemplate(name)) {
                    MapTemplateManager.addTree(zip, MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), name), TEMPLATE_PREFIX);
                }
            });
        }, error -> {
            release(name);
            return error == null
                    ? new Outcome(Code.OK, "ExportDone", name, "data/exports/" + fileName)
                    : new Outcome(Code.FAILED, "ExportFailed", name, error.getMessage());
        });
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
        if (inFlight(name) || inFlight(newName) || com.l299l.newbedwars.world.template.ArenaResetPipeline.isCopying(name)) {
            return busy(name);
        }
        Path mapSource;
        boolean fromWorld = !ArenaMaps.hasTemplate(name);
        if (!fromWorld) {
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
        if (!claim(newName)) return busy(newName);
        if (!claim(name)) {
            release(newName);
            return busy(name);
        }
        if (fromWorld) EDIT_LOCKED.add(key(name));
        Path copyTarget = ArenaMaps.worldFolder(newName);
        return runAsync(() -> {
            MapTemplateManager.createTemplate(ArenaMaps.mapsRoot(), newName, mapSource, excludes);
            ArenaTransfer.stripWorldIdentity(MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), newName));
            ArenaMaps.markDirty(newName);
            AtomicFileWriter.write(arenaFile, json, null, 0);
            ArenaMaps.restoreOffline(newName, copyTarget, excludes);
        }, error -> {
            release(newName);
            release(name);
            EDIT_LOCKED.remove(key(name));
            return finishCopy(name, newName, arenaFile, error);
        });
    }

    private static Outcome finishCopy(String name, String newName, Path arenaFile, Exception error) {
        if (error != null) {
            NewBedwars.plugin.getLogger().warning("Copy of arena " + name + " to " + newName + " failed: " + error);
            removeFiles(newName, newName, arenaFile);
            return new Outcome(Code.FAILED, "CopyFailed", newName, error.getMessage());
        }
        IArena loaded = null;
        try {
            loaded = NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(newName);
        } catch (RuntimeException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Copy of arena " + name + " to " + newName + " could not be loaded", e);
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
        Path tmp = exportsDir().resolve(".import-" + MapTemplateManager.timestamp() + "-" + System.nanoTime());
        AtomicReference<ArenaTransfer.Imported> read = new AtomicReference<>();
        return runAsync(() -> {
            try {
                MapTemplateManager.unzip(zip, tmp);
                read.set(ArenaTransfer.readImport(tmp));
            } catch (Exception e) {
                deleteTemp(tmp);
                throw e;
            }
        }, error -> {
            if (error == null) return null;
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Import of " + fileName + " failed", error);
            return new Outcome(Code.FAILED, "ImportFailed", fileName, error.getMessage());
        }).thenCompose(failed -> failed != null ? CompletableFuture.completedFuture(failed)
                : placeImport(fileName, read.get(), tmp));
    }

    private static CompletableFuture<Outcome> placeImport(String fileName, ArenaTransfer.Imported imported, Path tmp) {
        String name = imported.name();
        String worldName = imported.worldName();
        Path arenaFile = arenaFile(name);
        if (inFlight(name) || ArenaRegistry.contains(name) || Files.exists(arenaFile)
                || Files.exists(ArenaMaps.worldFolder(worldName))
                || Bukkit.getWorld(worldName) != null || ArenaMaps.hasTemplate(name) || worldUsedByArena(worldName)) {
            Bukkit.getScheduler().runTaskAsynchronously(NewBedwars.plugin, () -> deleteTemp(tmp));
            return done(Code.NAME_TAKEN, "ImportFailed", fileName,
                    "an arena or world named '" + name + "' / '" + worldName + "' already exists");
        }
        if (!claim(name)) {
            Bukkit.getScheduler().runTaskAsynchronously(NewBedwars.plugin, () -> deleteTemp(tmp));
            return busy(name);
        }
        Path importTarget = ArenaMaps.worldFolder(worldName);
        List<String> importExcludes = List.copyOf(Properties.RollbackTemplateExcludes);
        return runAsync(() -> {
            try {
                Path mapTemplate = MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), name);
                Files.createDirectories(mapTemplate.getParent());
                MapTemplateManager.copyTree(imported.template(), mapTemplate, List.of());
                ArenaTransfer.stripWorldIdentity(mapTemplate);
                ArenaMaps.markDirty(name);
                AtomicFileWriter.write(arenaFile, JsonUtils.beautifyJson(imported.json().toString()), null, 0);
                ArenaMaps.restoreOffline(name, importTarget, importExcludes);
            } finally {
                deleteTemp(tmp);
            }
        }, error -> {
            release(name);
            if (error != null) {
                NewBedwars.plugin.getLogger().log(Level.WARNING, "Import of " + fileName + " failed", error);
                removeFiles(name, worldName, arenaFile);
                return new Outcome(Code.FAILED, "ImportFailed", fileName, error.getMessage());
            }
            IArena loaded;
            try {
                loaded = NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(name);
            } catch (RuntimeException e) {
                NewBedwars.plugin.getLogger().log(Level.WARNING, "Import of " + fileName + " failed", e);
                loaded = null;
            }
            if (loaded == null) {
                removeFiles(name, worldName, arenaFile);
                return new Outcome(Code.FAILED, "ImportFailed", name, "the imported arena could not be loaded");
            }
            return new Outcome(Code.OK, "ImportDone", name, null);
        });
    }

    private static boolean worldUsedByArena(String worldName) {
        for (IArena a : ArenaRegistry.all()) {
            if (a.getArenaWorld() != null && a.getArenaWorld().getName().equalsIgnoreCase(worldName)) return true;
        }
        return false;
    }

    private static void deleteTemp(Path tmp) {
        try {
            MapTemplateManager.deleteTree(tmp);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not delete " + tmp, e);
        }
    }

    public static CompletableFuture<Outcome> delete(String arenaName) {
        IArena arena = ArenaRegistry.byName(arenaName);
        if (arena == null) return notFound(arenaName);
        String name = arena.getArenaName();
        if (inGame(arena) || (arena.isEnabled() && arena.status() == GameStatus.restarting) || inFlight(name)
                || com.l299l.newbedwars.world.template.ArenaResetPipeline.isCopying(name)) {
            return done(Code.BUSY, "ArenaCannotBeDeleted", name, null);
        }
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

        boolean loaded = Bukkit.getWorld(arenaWorld.getUID()) != null;
        if (loaded) {
            arenaWorld.setAutoSave(false);
            if (!Bukkit.unloadWorld(arenaWorld, false)) return done(Code.FAILED, "ArenaDeleteFailed", name, null);
        }
        ArenaRegistry.unregister(name, arenaWorld);
        claim(name);

        IOExecutor.flush();
        java.util.concurrent.atomic.AtomicBoolean complete = new java.util.concurrent.atomic.AtomicBoolean(true);
        Path arenaFile = arenaFile(name);
        Path maps = ArenaMaps.mapsRoot().resolve(name);
        return runAsync(() -> {
            if (!deleteQuietly(worldFolder)) complete.set(false);
            if (!deleteQuietly(arenaFile)) complete.set(false);
            if (!deleteQuietly(maps)) complete.set(false);
        }, error -> {
            release(name);
            if (error != null || !complete.get()) return new Outcome(Code.PARTIAL, "ArenaDeletedPartially", name, null);
            ArenaGroupManager groups = ArenaGroupManager.get();
            if (groups != null) groups.removeArena(name);
            return new Outcome(Code.OK, "ArenaDeleted", name, null);
        });
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
            com.l299l.newbedwars.world.template.BackgroundTasks.started();
            try {
                task.run();
            } catch (Exception e) {
                error = e;
            } catch (Throwable t) {
                NewBedwars.plugin.getLogger().log(Level.SEVERE, "Arena maintenance task failed", t);
                error = new IOException(t.toString(), t);
            } finally {
                com.l299l.newbedwars.world.template.BackgroundTasks.finished();
            }
            Exception result = error;
            boolean scheduled = com.l299l.newbedwars.utils.MainThread.run(NewBedwars.plugin, () -> {
                try {
                    future.complete(finish.apply(result));
                } catch (RuntimeException e) {
                    future.completeExceptionally(e);
                }
            });
            if (!scheduled) future.completeExceptionally(new IllegalStateException("NewBedwars is shutting down"));
        });
        return future;
    }
}
