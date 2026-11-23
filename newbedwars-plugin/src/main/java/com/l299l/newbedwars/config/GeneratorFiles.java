package com.l299l.newbedwars.config;

import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class GeneratorFiles {
    public static final int MAX_BYTES = 64 * 1024;
    public static final String DEFAULT_ID = "DefaultGenerators";

    private GeneratorFiles() {}

    public static boolean isBuiltIn(String id) {
        return DEFAULT_ID.equals(id) || BuiltInModes.NAMES.contains(id);
    }

    public static Map<String, Path> scan(Path folder) throws IOException {
        Map<String, Path> out = new TreeMap<>();
        if (!Files.isDirectory(folder)) return out;
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files.sorted()::iterator) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".yml") || !Files.isRegularFile(file)) continue;
                Object id = idOf(Files.readString(file, StandardCharsets.UTF_8));
                if (id instanceof String text) out.putIfAbsent(text, file);
            }
        }
        return out;
    }

    private static Object idOf(String yaml) {
        try {
            Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
            return root instanceof Map<?, ?> map ? map.get("ConfigurationId") : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static List<String> validate(String id, String yaml) {
        List<String> problems = new ArrayList<>();
        if (yaml == null || yaml.isBlank()) return List.of("the config text is empty");
        if (yaml.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) return List.of("the config is larger than 64 KB");
        Object root;
        try {
            root = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
        } catch (RuntimeException e) {
            return List.of("not valid YAML: " + firstLine(e.getMessage()));
        }
        if (!(root instanceof Map<?, ?> map)) return List.of("the config must be a YAML section");
        if (!id.equals(map.get("ConfigurationId") instanceof String text ? text : null)) {
            return List.of("ConfigurationId must be " + id);
        }
        YamlConfiguration conf = new YamlConfiguration();
        try {
            conf.loadFromString(yaml);
        } catch (InvalidConfigurationException | RuntimeException e) {
            return List.of("not valid YAML: " + firstLine(e.getMessage()));
        }
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.setLevel(Level.ALL);
        log.addHandler(new Handler() {
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) problems.add(record.getMessage());
            }

            public void flush() {
            }

            public void close() {
            }
        });
        try {
            new GeneratorLeveling(conf, log);
        } catch (RuntimeException e) {
            problems.add(e.getMessage() == null ? "invalid generator config" : e.getMessage());
        }
        return problems;
    }

    private static String firstLine(String text) {
        if (text == null) return "unreadable";
        int cut = text.indexOf('\n');
        return cut < 0 ? text : text.substring(0, cut);
    }
}
