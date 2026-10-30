package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataManager;
import com.l299l.newbedwars.config.data.json.arenas.ArenaMigrator;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.config.data.json.JsonUtils;
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
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * One class for the arena file/map maintenance commands, selected by {@link Action}:
 * backup, backups, restore, export, import, copy, resetmap.
 */
public class ArenaMaintenanceCommand extends BwCommand {
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_\\-]{1,64}");
    private static final String ARENA_ENTRY = "arena.json";
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
        runAsync(() -> {
            MapTemplateManager.zipTemplate(ArenaMaps.mapsRoot(), name, zip);
        }, error -> {
            if (error == null) reply(player, "BackupCreated", name, id);
            else reply(player, "BackupFailed", name, error.getMessage());
        });
    }

    private void doList(Player player, IArena arena) {
        String name = arena.getArenaName();
        try {
            TreeSet<String> ids = new TreeSet<>(Comparator.reverseOrder());
            for (Path p : MapTemplateManager.listBackups(ArenaMaps.arenaBackupDir(name), ".json")) ids.add(baseName(p));
            for (Path p : MapTemplateManager.listBackups(ArenaMaps.mapBackupDir(name), ".zip")) ids.add(baseName(p));
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
            com.l299l.newbedwars.config.data.IOExecutor.flush();
            String worldName = manager.unloadArena(name);
            if (worldName == null) {
                reply(player, "RestoreFailed", name, "the arena world could not be unloaded");
                return;
            }
            if (json != null) {
                Path target = NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("arenas").resolve(name + ".json");
                // the current file is kept as a normal backup
                AtomicFileWriter.write(target, Files.readString(json, StandardCharsets.UTF_8),
                        ArenaMaps.arenaBackupDir(name), Math.max(1, com.l299l.newbedwars.config.properties.Properties.BackupsKeep));
            }
            if (zip != null) {
                MapTemplateManager.restoreTemplateFromZip(ArenaMaps.mapsRoot(), name, zip);
            }
            if (ArenaMaps.hasTemplate(name)) {
                ArenaMaps.restoreOffline(name, worldName);
            }
            IArena loaded = manager.loadArena(name);
            reply(player, loaded != null ? "RestoreDone" : "RestoreFailed", name, loaded != null ? null : "the restored arena could not be loaded");
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().warning("Restore of arena " + name + " failed: " + e);
            reply(player, "RestoreFailed", name, e.getMessage());
            NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(name);
        }
    }

    private Path pickBackup(Path dir, String id, String extension) throws IOException {
        if (id == null) {
            List<Path> all = MapTemplateManager.listBackups(dir, extension);
            return all.isEmpty() ? null : all.get(0);
        }
        if (!SAFE_NAME.matcher(id).matches()) return null;
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
                zip.putNextEntry(new ZipEntry(ARENA_ENTRY));
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
        if (!SAFE_NAME.matcher(newName).matches()) {
            reply(player, "CopyFailed", newName, "use only letters, digits, _ and -");
            return;
        }
        Path arenaFile = NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("arenas").resolve(newName + ".json");
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
        List<String> excludes = List.copyOf(com.l299l.newbedwars.config.properties.Properties.RollbackTemplateExcludes);
        runAsync(() -> {
            MapTemplateManager.createTemplate(ArenaMaps.mapsRoot(), newName, mapSource, excludes);
            // Paper's world identity: a copy carrying it is refused as a duplicate of the source world.
            Path template = MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), newName);
            Files.deleteIfExists(template.resolve("data").resolve("paper").resolve("metadata.dat"));
            Files.deleteIfExists(template.resolve("uid.dat"));
            AtomicFileWriter.write(arenaFile, json, null, 0);
        }, error -> {
            if (error != null) {
                NewBedwars.plugin.getLogger().warning("Copy of arena " + name + " to " + newName + " failed: " + error);
                removeCopy(newName, arenaFile);
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
                removeCopy(newName, arenaFile);
                reply(player, "CopyFailed", newName, "the copy could not be loaded (see the console)");
                return;
            }
            com.l299l.newbedwars.arena.group.ArenaGroupManager groups = com.l299l.newbedwars.arena.group.ArenaGroupManager.get();
            com.l299l.newbedwars.arena.group.ArenaGroup group = groups == null ? null : groups.groupOf(name);
            if (group != null) {
                group.add(newName);
                groups.changed();
            }
            reply(player, "CopyDone", newName, name + (group != null ? ", group " + group.getName() : ""));
        });
    }

    /** Undoes a failed copy: arena file, map template and world folder of {@code newName}. */
    private static void removeCopy(String newName, Path arenaFile) {
        try {
            World world = Bukkit.getWorld(newName);
            if (world != null) Bukkit.unloadWorld(world, false);
            Files.deleteIfExists(arenaFile);
            MapTemplateManager.deleteTree(MapTemplateManager.arenaRoot(ArenaMaps.mapsRoot(), newName));
            MapTemplateManager.deleteTree(ArenaMaps.worldFolder(newName));
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().warning("Could not clean up the failed copy " + newName + ": " + e.getMessage());
        }
    }

    private void doImport(Player player, String fileName) {
        Path zip = exportsDir().resolve(fileName).normalize();
        if (!zip.startsWith(exportsDir()) || !Files.isRegularFile(zip)) {
            reply(player, "ImportFailed", fileName, "file not found in data/exports");
            return;
        }
        Path tmp = exportsDir().resolve(".import-" + MapTemplateManager.timestamp());
        try {
            MapTemplateManager.unzip(zip, tmp);
            Path jsonFile = tmp.resolve(ARENA_ENTRY);
            if (!Files.isRegularFile(jsonFile)) throw new IOException("the zip has no " + ARENA_ENTRY);
            JsonObject json = ArenaMigrator.migrate(new Gson().fromJson(Files.readString(jsonFile, StandardCharsets.UTF_8), JsonObject.class));
            String name = json.get("arenaName").getAsString();
            String worldName = json.get("worldName").getAsString();
            if (!SAFE_NAME.matcher(name).matches() || !SAFE_NAME.matcher(worldName).matches()) {
                throw new IOException("the arena or world name contains illegal characters");
            }
            Path arenaFile = NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("arenas").resolve(name + ".json");
            if (ArenaRegistry.contains(name) || Files.exists(arenaFile) || Files.exists(ArenaMaps.worldFolder(worldName))
                    || Bukkit.getWorld(worldName) != null) {
                throw new IOException("an arena or world named '" + name + "' / '" + worldName + "' already exists");
            }
            Path template = tmp.resolve("template");
            if (!MapTemplateManager.isWorldFolder(template)) throw new IOException("the zip has no map");
            json.addProperty("enabled", false);
            json.addProperty("setup", "NO_SETUP");
            Path mapTemplate = MapTemplateManager.templateDir(ArenaMaps.mapsRoot(), name);
            Files.createDirectories(mapTemplate.getParent());
            MapTemplateManager.copyTree(template, mapTemplate, List.of());
            AtomicFileWriter.write(arenaFile, JsonUtils.beautifyJson(json.toString()), null, 0);
            ArenaMaps.restoreOffline(name, worldName);
            IArena loaded = NewBedwars.plugin.getDataManager().getArenaDataManager().loadArena(name);
            reply(player, loaded != null ? "ImportDone" : "ImportFailed", name, loaded != null ? null : "the imported arena could not be loaded");
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().warning("Import of " + fileName + " failed: " + e);
            reply(player, "ImportFailed", fileName, e.getMessage());
        } finally {
            try {
                MapTemplateManager.deleteTree(tmp);
            } catch (IOException ignored) {
            }
        }
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
    private static void runAsync(IoTask task, java.util.function.Consumer<Exception> done) {
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
                TreeSet<String> ids = new TreeSet<>(Comparator.reverseOrder());
                for (Path p : MapTemplateManager.listBackups(ArenaMaps.arenaBackupDir(args[2]), ".json")) ids.add(baseName(p));
                for (Path p : MapTemplateManager.listBackups(ArenaMaps.mapBackupDir(args[2]), ".zip")) ids.add(baseName(p));
                return new ArrayList<>(ids);
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
