package com.l299l.newbedwars.config;

import com.l299l.newbedwars.NewBedwars;
import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class Updater extends PluginFiles {
    private final Map<Language, FileConfiguration> langConfs = new EnumMap<>(Language.class);
    private final Map<Language, File> langFiles = new EnumMap<>(Language.class);
    private FileConfiguration bossBarsConf = null;
    private FileConfiguration scoreboardsConf = null;
    private FileConfiguration tablistsConf = null;
    private final List<FileConfiguration> generatorsConfigurations = new ArrayList<>();
    private FileConfiguration defaultGeneratorsConf = null;
    private final ClassLoader classLoader;
    private Map<String, Object> legacyUpgradePrices;
    private final File bossBarsFile = new File(NewBedwars.plugin.getDataFolder(), "bossBars.yml");
    private final File scoreboardFile = new File(NewBedwars.plugin.getDataFolder(), "scoreboards.yml");
    private final File tablistFile = new File(NewBedwars.plugin.getDataFolder(), "tablists.yml");
    private final File generatorsFolder = new File(NewBedwars.plugin.getDataFolder(), "generators");
    private final File generatorsFile = new File(NewBedwars.plugin.getDataFolder(), "generators/defaultGenerators.yml");
    private final String error = ChatColor.RED + "[NewBedwars]: " + ChatColor.DARK_RED + "The language file could not be saved!";

    public Updater() {
        classLoader = getClass().getClassLoader();
        for (Language lang : Language.values()) {
            langFiles.put(lang, new File(NewBedwars.plugin.getDataFolder(), "language/" + lang.getCode() + ".yml"));
        }
    }

    public void updateConf() {
        File configFile = new File(NewBedwars.plugin.getDataFolder(), "config.yml");
        if (!usable(configFile)) {
            NewBedwars.plugin.reloadConfig();
            loadModes(null, null);
            return;
        }
        NewBedwars.plugin.saveDefaultConfig();
        org.bukkit.configuration.ConfigurationSection legacy = YamlConfiguration.loadConfiguration(configFile)
                .getConfigurationSection("UpgradePrices");
        if (legacy != null) legacyUpgradePrices = legacy.getValues(false);
        Map<String, Object> raw = null;
        try {
            raw = ModesFile.parse(java.nio.file.Files.readString(configFile.toPath(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not read config.yml", e);
        }
        loadModes(raw == null ? null : raw.get(ModesFile.PHASES), raw == null ? null : raw.get(ModesFile.MODES));
        try {
            ConfigUpdater.update(NewBedwars.plugin, "config.yml", configFile, Collections.emptyList());
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not update config.yml", e);
        }
        NewBedwars.plugin.reloadConfig();
    }

    public void reloadLanguages() {
        for (Language lang : Language.values()) {
            File file = langFiles.get(lang);
            if (!usable(file)) continue;
            String resource = lang.getCode() + ".yml";
            FileConfiguration conf = loadOrDefault(file, resource);
            if (conf != null) langConfs.put(lang, conf);
            Map<String, String> previous = conf == null ? Map.of() : topLevelTexts(conf);
            save(conf, file);
            List<String> ignored = conf != null && conf.contains("CustomItemsNames")
                    ? Collections.singletonList("CustomItemsNames") : Collections.emptyList();
            update(resource, file, ignored);
            if (lang != Language.English) fillMissingTexts(lang, file, previous);
            conf = loadOrDefault(file, resource);
            if (conf != null) langConfs.put(lang, conf);
        }
    }

    private void fillMissingTexts(Language lang, File file, Map<String, String> previous) {
        try {
            Map<String, String> english = topLevelTexts(bundledConf("en.yml"));
            if (english.isEmpty()) return;
            Map<String, String> changes = LanguageFill.plan(english,
                    topLevelTexts(bundledConf(lang.getCode() + ".yml")),
                    topLevelTexts(YamlConfiguration.loadConfiguration(file)), previous);
            if (changes.isEmpty()) return;
            String text = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            java.nio.file.Files.writeString(file.toPath(), LanguageFill.write(text, changes), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not fill missing texts in " + file.getPath(), e);
        }
    }

    private FileConfiguration bundledConf(String resource) {
        InputStream stream = getFilesResource(resource, getMessagesClassLoader());
        if (stream == null) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static Map<String, String> topLevelTexts(FileConfiguration conf) {
        Map<String, String> texts = new java.util.LinkedHashMap<>();
        for (String key : conf.getKeys(false)) {
            if (conf.isString(key)) texts.put(key, conf.getString(key));
        }
        return texts;
    }

    public void reloadBossBars() {
        bossBarsConf = refresh("bossBars.yml", bossBarsFile, bossBarsConf, "BossBars");
    }

    public void reloadScoreboards() {
        scoreboardsConf = refresh("scoreboards.yml", scoreboardFile, scoreboardsConf, "Scoreboards");
    }

    public void reloadTablists() {
        tablistsConf = refresh("tablists.yml", tablistFile, tablistsConf, "Layouts");
    }

    public void reloadGeneratorsConfigurations() {
        if (!usable(generatorsFile)) {
            reloadGenerators();
            return;
        }
        reloadGenerators();
        save(defaultGeneratorsConf, generatorsFile);
        List<String> keep = defaultGeneratorsConf != null && defaultGeneratorsConf.contains("CustomGenerators")
                ? Collections.singletonList("CustomGenerators") : Collections.emptyList();
        update("generators/defaultGenerators.yml", generatorsFile, keep);
        reloadGenerators();
    }

    public Map<String, Object> takeLegacyUpgradePrices() {
        Map<String, Object> legacy = legacyUpgradePrices;
        legacyUpgradePrices = null;
        return legacy;
    }

    public Map<Language, FileConfiguration> getLangConfs() {
        return langConfs;
    }

    public Map<Language, File> getLangFiles() {
        return langFiles;
    }

    public FileConfiguration getBossBarsConf() {
        return bossBarsConf;
    }

    public FileConfiguration getScoreboardsConf() {
        return scoreboardsConf;
    }

    public FileConfiguration getTablistsConf() {
        return tablistsConf;
    }

    public List<FileConfiguration> getGeneratorsConfigurations() {
        return generatorsConfigurations;
    }

    private FileConfiguration refresh(String resource, File file, FileConfiguration current, String container) {
        if (!usable(file)) return current;
        FileConfiguration conf = loadOrDefault(file, resource);
        if (conf == null) conf = current;
        save(conf, file);
        Map<String, Object> custom = customEntries(conf, resource, container);
        update(resource, file, Collections.emptyList());
        restoreCustomEntries(file, container, custom);
        FileConfiguration updated = loadOrDefault(file, resource);
        return updated != null ? updated : conf;
    }

    private Map<String, Object> customEntries(FileConfiguration conf, String resource, String container) {
        Map<String, Object> custom = new java.util.LinkedHashMap<>();
        if (conf == null) return custom;
        org.bukkit.configuration.ConfigurationSection user = conf.getConfigurationSection(container);
        if (user == null) return custom;
        InputStream stream = getFilesResource(resource, getMessagesClassLoader());
        if (stream == null) return custom;
        FileConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        org.bukkit.configuration.ConfigurationSection builtIn = defaults.getConfigurationSection(container);
        for (String key : user.getKeys(false)) {
            if (builtIn != null && builtIn.contains(key)) continue;
            custom.put(key, user.get(key));
        }
        return custom;
    }

    private void restoreCustomEntries(File file, String container, Map<String, Object> custom) {
        if (custom.isEmpty()) return;
        try {
            FileConfiguration after = YamlConfiguration.loadConfiguration(file);
            YamlConfiguration only = new YamlConfiguration();
            for (Map.Entry<String, Object> e : custom.entrySet()) {
                if (!after.contains(container + "." + e.getKey())) only.set(e.getKey(), e.getValue());
            }
            if (only.getKeys(false).isEmpty()) return;
            String text = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (!lastTopLevelKeyIs(text, container)) {
                NewBedwars.plugin.getLogger().warning("Could not keep your custom entries in " + file.getName()
                        + ": the section '" + container + "' is not the last section of the file");
                return;
            }
            StringBuilder out = new StringBuilder(text);
            if (!text.endsWith("\n")) out.append('\n');
            for (String line : only.saveToString().split("\n")) {
                if (!line.isEmpty()) out.append("  ").append(line).append('\n');
            }
            java.nio.file.Files.writeString(file.toPath(), out.toString(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not keep your custom entries in " + file.getName(), e);
        }
    }

    private static boolean lastTopLevelKeyIs(String text, String container) {
        String last = null;
        for (String line : text.split("\n")) {
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == ' ' || line.charAt(0) == '\t') continue;
            int colon = line.indexOf(':');
            if (colon > 0) last = line.substring(0, colon).trim();
        }
        return container.equals(last);
    }

    private boolean usable(File file) {
        boolean valid = YamlGuard.ensureValid(file, NewBedwars.plugin.getLogger());
        if (valid) backupOnce(file);
        return valid || !file.exists();
    }

    private void backupOnce(File file) {
        if (file == null || !file.isFile()) return;
        java.nio.file.Path folder = NewBedwars.plugin.getDataFolder().toPath();
        java.nio.file.Path target = folder.resolve("data").resolve("backups").resolve("config")
                .resolve(NewBedwars.plugin.getDescription().getVersion()).resolve(folder.relativize(file.toPath()).toString());
        if (java.nio.file.Files.exists(target)) return;
        try {
            java.nio.file.Files.createDirectories(target.getParent());
            java.nio.file.Files.copy(file.toPath(), target);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not back up " + file.getPath() + " before updating it", e);
        }
    }

    private FileConfiguration loadOrDefault(File file, String resource) {
        if (file != null && file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        InputStream stream = getFilesResource(resource, getMessagesClassLoader());
        return stream == null ? null : YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private void save(FileConfiguration conf, File file) {
        if (conf == null) return;
        try {
            conf.save(file);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not save " + file.getPath(), e);
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }
    }

    private void update(String resource, File file, List<String> ignoredSections) {
        try {
            ConfigUpdater.update(NewBedwars.plugin, resource, file, ignoredSections);
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not update " + file.getPath() + " from " + resource, e);
        }
    }

    private void reloadGenerators() {
        generatorsConfigurations.clear();
        defaultGeneratorsConf = null;
        if (!generatorsFolder.exists()) {
            generatorsFolder.mkdirs();
        }
        copyBuiltInModes();
        FileConfiguration generatorsConf = null;
        if (generatorsFile.exists()) {
            generatorsConf = YamlConfiguration.loadConfiguration(generatorsFile);
            for (File file : Objects.requireNonNull(generatorsFolder.listFiles())) {
                if (file.getName().endsWith(".yml") && !file.getName().equals("defaultGenerators.yml")) {
                    generatorsConfigurations.add(YamlConfiguration.loadConfiguration(file));
                }
            }
        } else {
            generatorsConf = loadOrDefault(null, "generators/defaultGenerators.yml");
        }
        defaultGeneratorsConf = generatorsConf;
        if (generatorsConf != null) generatorsConfigurations.add(generatorsConf);
    }

    private void loadModes(Object legacyPhases, Object legacyModes) {
        File modesFile = new File(NewBedwars.plugin.getDataFolder(), ModesFile.FILE);
        usable(modesFile);
        String bundled = "";
        try (java.io.InputStream in = NewBedwars.plugin.getResource(ModesFile.FILE)) {
            if (in != null) bundled = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not read the bundled " + ModesFile.FILE, e);
        }
        new ModesFile(NewBedwars.plugin.getDataFolder().toPath(), bundled, NewBedwars.plugin.getLogger())
                .load(legacyPhases, legacyModes);
    }

    private void copyBuiltInModes() {
        for (String mode : BuiltInModes.NAMES) {
            if (new File(generatorsFolder, mode + ".yml").exists()) continue;
            try {
                NewBedwars.plugin.saveResource(BuiltInModes.generatorResource(mode), false);
            } catch (IllegalArgumentException e) {
                NewBedwars.plugin.getLogger().warning("Built-in generator preset " + mode + ".yml is missing from the plugin jar");
            }
        }
    }

    protected final ClassLoader getMessagesClassLoader() {
        return this.classLoader;
    }
}
