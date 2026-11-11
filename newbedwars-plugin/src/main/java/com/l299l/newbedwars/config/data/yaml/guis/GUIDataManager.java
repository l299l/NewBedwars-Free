package com.l299l.newbedwars.config.data.yaml.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUISave;
import com.l299l.newbedwars.world.template.MapTemplateManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class GUIDataManager {
    private final Path guiDir;
    private final Path backupRoot;
    private final Function<String, CustomItem> items;
    private final Consumer<Runnable> io;
    private final Logger log;
    private final int keep;
    private final Map<String, GUISave> guis = new LinkedHashMap<>();
    private final Map<String, Path> files = new LinkedHashMap<>();
    private final Map<String, String> brokenFiles = new LinkedHashMap<>();

    public GUIDataManager() {
        this(NewBedwars.plugin.getDataFolder().toPath(),
                name -> NewBedwars.plugin.getCustomItemManager().getCustomItem(name),
                IOExecutor::submit, NewBedwars.plugin.getLogger(), Math.max(1, Properties.BackupsKeep));
    }

    public GUIDataManager(Path dataFolder, Function<String, CustomItem> items, Consumer<Runnable> io, Logger log, int keep) {
        this.guiDir = dataFolder.resolve("data/guis");
        this.backupRoot = dataFolder.resolve("data/backups/shop/guis");
        this.items = items;
        this.io = io;
        this.log = log;
        this.keep = keep;
    }

    public void load() {
        guis.clear();
        files.clear();
        brokenFiles.clear();
        if (!Files.isDirectory(guiDir)) {
            try {
                Files.createDirectories(guiDir);
                copyExample("exampleShop.yml");
                copyExample("exampleUpgrade.yml");
            } catch (IOException e) {
                log.log(Level.SEVERE, "Could not copy the default GUI files", e);
            }
        }
        List<Path> paths;
        try (Stream<Path> s = Files.list(guiDir)) {
            paths = s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".yml") && Files.isRegularFile(p))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not list " + guiDir, e);
            return;
        }
        for (Path file : paths) {
            try {
                GuiDefinition def = GuiCodec.parse(Files.readString(file, StandardCharsets.UTF_8));
                if (guis.containsKey(def.id())) {
                    String msg = "GuiId " + def.id() + " is already used by " + files.get(def.id()).getFileName();
                    brokenFiles.put(file.getFileName().toString(), msg);
                    log.warning("Skipped GUI file " + file.getFileName() + ": " + msg);
                    continue;
                }
                put(def, file);
            } catch (Exception e) {
                brokenFiles.put(file.getFileName().toString(), String.valueOf(e.getMessage()));
                log.log(Level.SEVERE, "Could not load GUI file " + file.getFileName() + ": " + e.getMessage());
            }
        }
    }

    private void put(GuiDefinition def, Path file) {
        GUISave save = new GuiResolver(items).resolve(def);
        guis.put(def.id(), save);
        files.put(def.id(), file);
        for (String problem : save.getProblems()) {
            log.warning("GUI " + def.id() + " (" + file.getFileName() + "): " + problem);
        }
    }

    public void rebuild() {
        for (Map.Entry<String, GUISave> e : new ArrayList<>(guis.entrySet())) {
            e.setValue(new GuiResolver(items).resolve(e.getValue().getDefinition()));
        }
    }

    public GUISave save(GuiDefinition def) {
        Path file = files.get(def.id());
        if (file == null) file = guiDir.resolve(def.id() + ".yml");
        GUISave save = new GuiResolver(items).resolve(def);
        guis.put(def.id(), save);
        files.put(def.id(), file);
        Path target = file;
        Path backups = backupRoot.resolve(def.id());
        String yaml = GuiCodec.toYaml(def);
        io.accept(() -> {
            try {
                AtomicFileWriter.write(target, yaml, backups, keep, ".yml");
            } catch (IOException e) {
                log.log(Level.SEVERE, "Could not save GUI file " + target, e);
            }
        });
        return save;
    }

    public boolean delete(String id) {
        GUISave removed = guis.remove(id);
        Path file = files.remove(id);
        if (removed == null || file == null) return false;
        Path backups = backupRoot.resolve(id);
        io.accept(() -> {
            try {
                if (Files.isRegularFile(file)) {
                    Files.createDirectories(backups);
                    Files.copy(file, backups.resolve(MapTemplateManager.timestamp() + "-deleted.yml"), StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(file);
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Could not delete GUI file " + file, e);
            }
        });
        return true;
    }

    public boolean fileTaken(String id) {
        return !files.containsKey(id) && Files.exists(guiDir.resolve(id + ".yml"));
    }

    public List<GUISave> getGuiSaves() {
        return new ArrayList<>(guis.values());
    }

    public GUISave get(String id) {
        return guis.get(id);
    }

    public Map<String, String> getBrokenFiles() {
        return Map.copyOf(brokenFiles);
    }

    private void copyExample(String name) throws IOException {
        try (InputStream in = GUIDataManager.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new IOException(name + " is missing in the plugin jar");
            Files.copy(in, guiDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
