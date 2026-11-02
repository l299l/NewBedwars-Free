package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataManager;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.config.data.json.JsonUtils;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.world.template.ArenaMaps;
import com.l299l.newbedwars.world.template.MapTemplateManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArenaMaintenanceCommand extends BwCommand {
    private static final String TEMPLATE_PREFIX = "template/";

    public enum Action {
        BACKUP("backup", "Back up the arena file and map now.", "<arenaName>", "castle", 3, 3),
        BACKUPS("backups", "List the backups of an arena.", "<arenaName>", "castle", 3, 3),
        RESTORE("restore", "Restore an arena (file and map) from a backup.", "<arenaName> [backupId]", "castle", 3, 4),
        EXPORT("export", "Export an arena (file and map) to a zip.", "<arenaName>", "castle", 3, 3),
        IMPORT("import", "Import an arena from a zip in data/exports (loaded disabled).", "<fileName>", "castle-20261027.zip", 3, 3),
        COPY("copy", "Copy an arena (settings and map) to a new arena; the copy joins the same group.", "<arenaName> <newArenaName>", "castle castle2", 4, 4),
        RESETMAP("resetmap", "Put the arena map back to its saved state now.", "<arenaName>", "castle", 3, 3);

        final String command;
        final String description;
        final String arguments;
        final String example;
        final int minArgs;
        final int maxArgs;

        Action(String command, String description, String arguments, String example, int minArgs, int maxArgs) {
            this.command = command;
            this.description = description;
            this.arguments = arguments;
            this.example = example;
            this.minArgs = minArgs;
            this.maxArgs = maxArgs;
        }
    }

    private final Action action;

    public ArenaMaintenanceCommand(Action action) {
        this.action = action;
    }

    @Override
    public String getName() {
        return action.command;
    }

    @Override
    public String getDescription() {
        return action.description;
    }

    @Override
    public String getSyntax() {
        return "/bw arena " + action.command + " " + action.arguments;
    }

    @Override
    public String getExample() {
        return "/bw arena " + action.command + " " + action.example;
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (!player.hasPermission("newbedwars.bw.arena." + action.command)) {
            msg.send(player, "NoPermissions");
            return;
        }
        if (args.length < action.minArgs || args.length > action.maxArgs) {
            usage(player);
            return;
        }
        if (action == Action.IMPORT) {
            doImport(player, args[2]);
            return;
        }
        IArena arena = ArenaRegistry.byName(args[2]);
        if (arena == null) {
            msg.send(player, "ArenaNotExists");
            return;
        }
        switch (action) {
            case BACKUP -> doBackup(player, arena);
            case BACKUPS -> doList(player, arena);
            case RESTORE -> doRestore(player, arena, args.length == 4 ? args[3] : null);
            case EXPORT -> doExport(player, arena);
            case COPY -> doCopy(player, arena, args[3]);
            case RESETMAP -> doResetMap(player, arena);
            default -> { }
        }
    }

    private boolean busy(IArena arena) {
        GameStatus s = arena.status();
        return !arena.getPlayers().isEmpty() || !arena.getSpectators().isEmpty()
                || s == GameStatus.playing || s == GameStatus.starting || s == GameStatus.ending
                || (arena.isEnabled() && s != GameStatus.waiting);
    }

    private void reply(Player player, String key, String arena, String extra) {
        msg.send(player, key, Map.of(
                "/arenaname/", arena,
                "/info/", extra == null ? "" : extra));
    }

    private void doBackup(Player player, IArena arena) {
        String name = arena.getArenaName();
        if (busy(arena)) {
            reply(player, "ArenaBusy", name, null);
            return;
        }
        String id = MapTemplateManager.timestamp();
        try {
            String json = JsonUtils.beautifyJson(arena.toJson());
            Path dir = ArenaMaps.arenaBackupDir(name);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(id + ".json"), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            reply(player, "BackupFailed", name, e.getMessage());
            return;
        }
        if (!ArenaMaps.hasTemplate(name)) {
            reply(player, "BackupCreated", name, id);
            return;
        }
        Path zip = ArenaMaps.mapBackupDir(name).resolve(id + ".zip");
        runAsync(() -> MapTemplateManager.zipTemplate(ArenaMaps.mapsRoot(), name, zip), error -> {
            if (error == null) reply(player, "BackupCreated", name, id);
            else reply(player, "BackupFailed", name, error.getMessage());
        });
    }

    private void doList(Player player, IArena arena) {
        String name = arena.getArenaName();
        try {
            Set<String> ids = backupIds(name);
            if (ids.isEmpty()) {
                reply(player, "BackupListEmpty", name, null);
                return;
            }
            reply(player, "BackupListHeader", name, null);
            for (String id : ids) {
                boolean hasJson = Files.isRegularFile(ArenaMaps.arenaBackupDir(name).resolve(id + ".json"));
                boolean hasMap = Files.isRegularFile(ArenaMaps.mapBackupDir(name).resolve(id + ".zip"));
                player.sendMessage(msg.getMsg(player, "BackupListEntry").replace("/id/", id)
                        .replace("/parts/", (hasJson ? "arena" : "") + (hasJson && hasMap ? " + " : "") + (hasMap ? "map" : "")));
            }
        } catch (IOException e) {
            reply(player, "BackupFailed", name, e.getMessage());
        }
    }

    private void doRestore(Player player, IArena arena, String requestedId) {
        String name = arena.getArenaName();
        if (busy(arena)) {
            reply(player, "ArenaBusy", name, null);
            return;
        }
        try {
            Path json = pickBackup(ArenaMaps.arenaBackupDir(name), requestedId, ".json");
            Path zip = pickBackup(ArenaMaps.mapBackupDir(name), requestedId, ".zip");
            if (json == null && zip == null) {
                reply(player, "RestoreNothing", name, requestedId);
                return;
            }
            ArenaDataManager manager = NewBedwars.plugin.getDataManager().getArenaDataManager();
            IOExecutor.flush();
            String worldName = manager.unloadArena(name);
            if (worldName == null) {
                reply(player, "RestoreFailed", name, "the arena world could not be unloaded");
                return;
            }
            if (json != null) {
                AtomicFileWriter.write(arenaFile(name), Files.readString(json, StandardCharsets.UTF_8),
                        ArenaMaps.arenaBackupDir(name), Math.max(1, Properties.BackupsKeep));
            }
            if (zip != null) {
                MapTemplateManager.restoreTemplateFromZip(ArenaMaps.mapsRoot(), name, zip);
            }
            if (ArenaMaps.hasTemplate(name)) {
                ArenaMaps.restoreOffline(name, worldName);
            }
            IArena loaded = manager.loadArena(name);
            reply(player, loaded != null ? "RestoreDone" : "RestoreFailed", name, loaded != null ? null : "the restored arena could not be loaded");
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Restore of arena " + name + " failed", e);
            reply(player, "RestoreFailed", name, e.getMessage());
            if (NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(name) == null) {
                NewBedwars.plugin.getLogger().severe("Arena " + name + " could not be loaded again after the failed restore");
            }
        }
    }

    private Path pickBackup(Path dir, String id, String extension) throws IOException {
        if (id == null) {
            List<Path> all = MapTemplateManager.listBackups(dir, extension);
            return all.isEmpty() ? null : all.get(0);
        }
        if (!ArenaTransfer.isSafeName(id)) return null;
        Path file = dir.resolve(id + extension);
        return Files.isRegularFile(file) ? file : null;
    }

    private void doResetMap(Player player, IArena iArena) {
        String name = iArena.getArenaName();
        if (!(iArena instanceof Arena arena) || busy(iArena) || !arena.resetMap()) {
            reply(player, "ArenaBusy", name, null);
            return;
        }
        reply(player, "ResetMapStarted", name, null);
    }

    private void doExport(Player player, IArena arena) {
        String name = arena.getArenaName();
        if (busy(arena)) {
            reply(player, "ArenaBusy", name, null);
            return;
        }
        String json;
        try {
            json = JsonUtils.beautifyJson(arena.toJson());
        } catch (IOException e) {
            reply(player, "ExportFailed", name, e.getMessage());
            return;
        }
        String fileName = name + "-" + MapTemplateManager.timestamp() + ".zip";
        Path target = exportsDir().resolve(fileName);
        runAsync(() -> {
            Files.createDirectories(target.getParent());
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
                zip.putNextEntry(new ZipEntry(ArenaTransfer.ARENA_ENTRY));
                zip.write(json.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                if (ArenaMaps.hasTemplate(name)) {
                    MapTemplateManager.addTree(zip, MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), name), TEMPLATE_PREFIX);
                }
            }
        }, error -> {
            if (error == null) reply(player, "ExportDone", name, "data/exports/" + fileName);
            else reply(player, "ExportFailed", name, error.getMessage());
        });
    }

    private void doCopy(Player player, IArena source, String newName) {
        String name = source.getArenaName();
        if (!ArenaTransfer.isSafeName(newName)) {
            reply(player, "CopyFailed", newName, "use only letters, digits, _ and -");
            return;
        }
        Path arenaFile = arenaFile(newName);
        if (ArenaRegistry.find(newName) != null || Files.exists(arenaFile)
                || Bukkit.getWorld(newName) != null || Files.exists(ArenaMaps.worldFolder(newName))
                || MapTemplateManager.hasTemplate(ArenaMaps.mapsRoot(), newName)) {
            reply(player, "CopyFailed", newName, "an arena, map or world with that name already exists");
            return;
        }
        // The pristine template is copied when there is one; otherwise the saved world (only while nobody uses it).
        Path mapSource;
        if (ArenaMaps.hasTemplate(name)) {
            mapSource = MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), name);
        } else {
            if (busy(source)) {
                reply(player, "ArenaBusy", name, null);
                return;
            }
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
            reply(player, "CopyFailed", newName, e.getMessage());
            return;
        }
        List<String> excludes = List.copyOf(Properties.RollbackTemplateExcludes);
        runAsync(() -> {
            MapTemplateManager.createTemplate(ArenaMaps.mapsRoot(), newName, mapSource, excludes);
            ArenaTransfer.stripWorldIdentity(MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), newName));
            AtomicFileWriter.write(arenaFile, json, null, 0);
        }, error -> {
            if (error != null) {
                NewBedwars.plugin.getLogger().warning("Copy of arena " + name + " to " + newName + " failed: " + error);
                removeFiles(newName, newName, arenaFile);
                reply(player, "CopyFailed", newName, error.getMessage());
                return;
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
                reply(player, "CopyFailed", newName, "the copy could not be loaded (see the console)");
                return;
            }
            ArenaGroupManager groups = ArenaGroupManager.get();
            ArenaGroup group = groups == null ? null : groups.groupOf(name);
            if (group != null) {
                group.add(newName);
                groups.changed();
            }
            reply(player, "CopyDone", newName, name + (group != null ? ", group " + group.getName() : ""));
        });
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

    void doImport(Player player, String fileName) {
        Path zip = ArenaTransfer.resolveExport(exportsDir(), fileName);
        if (zip == null) {
            reply(player, "ImportFailed", fileName, "file not found in data/exports");
            return;
        }
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
                throw new IOException("an arena or world named '" + name + "' / '" + worldName + "' already exists");
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
                reply(player, "ImportFailed", name, "the imported arena could not be loaded");
                return;
            }
            reply(player, "ImportDone", name, null);
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Import of " + fileName + " failed", e);
            if (createdName != null) removeFiles(createdName, createdWorld, arenaFile(createdName));
            reply(player, "ImportFailed", fileName, e.getMessage());
        } finally {
            try {
                MapTemplateManager.deleteTree(tmp);
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not delete " + tmp, e);
            }
        }
    }

    private static Path arenaFile(String name) {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("arenas").resolve(name + ".json");
    }

    private static Set<String> backupIds(String name) throws IOException {
        TreeSet<String> ids = new TreeSet<>(Comparator.reverseOrder());
        for (Path p : MapTemplateManager.listBackups(ArenaMaps.arenaBackupDir(name), ".json")) ids.add(baseName(p));
        for (Path p : MapTemplateManager.listBackups(ArenaMaps.mapBackupDir(name), ".zip")) ids.add(baseName(p));
        return ids;
    }

    private static Path exportsDir() {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("exports").toAbsolutePath().normalize();
    }

    private static String baseName(Path p) {
        String n = p.getFileName().toString();
        return n.substring(0, n.lastIndexOf('.'));
    }

    private interface IoTask {
        void run() throws Exception;
    }

    /** Runs {@code task} off the main thread, then {@code done} on it ({@code null} = success). */
    private static void runAsync(IoTask task, Consumer<Exception> done) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Exception error = null;
                try {
                    task.run();
                } catch (Exception e) {
                    error = e;
                }
                Exception result = error;
                Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> done.accept(result));
            }
        }.runTaskAsynchronously(NewBedwars.plugin);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 3) {
            if (action == Action.IMPORT) {
                try (Stream<Path> s = Files.isDirectory(exportsDir()) ? Files.list(exportsDir()) : Stream.<Path>empty()) {
                    return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".zip")).toList();
                } catch (IOException e) {
                    return new ArrayList<>();
                }
            }
            return new ArrayList<>(ArenaRegistry.names());
        }
        if (args.length == 4 && action == Action.COPY) {
            return List.of(args[2] + "2");
        }
        if (args.length == 4 && action == Action.RESTORE && ArenaRegistry.contains(args[2])) {
            try {
                return new ArrayList<>(backupIds(args[2]));
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not list the backups of " + args[2], e);
            }
        }
        return null;
    }
}
