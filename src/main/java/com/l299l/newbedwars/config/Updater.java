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

public class Updater extends PluginFiles {
    private final Map<Language, FileConfiguration> langConfs = new EnumMap<>(Language.class);
    private final Map<Language, File> langFiles = new EnumMap<>(Language.class);
    private FileConfiguration bossBarsConf = null;
    private FileConfiguration scoreboardsConf = null;
    private final List<FileConfiguration> generatorsConfigurations = new ArrayList<>();
    private FileConfiguration defaultGeneratorsConf = null;
    private final ClassLoader classLoader;
    private final File bossBarsFile = new File(NewBedwars.plugin.getDataFolder(), "bossBars.yml");
    private final File scoreboardFile = new File(NewBedwars.plugin.getDataFolder(), "scoreboards.yml");
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
        NewBedwars.plugin.saveDefaultConfig();
        File configFile = new File(NewBedwars.plugin.getDataFolder(), "config.yml");
        try {
            ConfigUpdater.update(NewBedwars.plugin, "config.yml", configFile, Collections.emptyList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        NewBedwars.plugin.reloadConfig();
    }

    public void reloadLanguages() {
        for (Language lang : Language.values()) {
            File file = langFiles.get(lang);
            String resource = lang.getCode() + ".yml";
            FileConfiguration conf = loadOrDefault(file, resource);
            if (conf != null) langConfs.put(lang, conf);
            save(conf, file);
            List<String> ignored = conf != null && conf.contains("CustomItemsNames")
                    ? Collections.singletonList("CustomItemsNames") : Collections.emptyList();
            update(resource, file, ignored);
            conf = loadOrDefault(file, resource);
            if (conf != null) langConfs.put(lang, conf);
        }
    }

    public void reloadBossBars() {
        bossBarsConf = refresh("bossBars.yml", bossBarsFile, bossBarsConf);
    }

    public void reloadScoreboards() {
        scoreboardsConf = refresh("scoreboards.yml", scoreboardFile, scoreboardsConf);
    }

    public void reloadGeneratorsConfigurations() {
        reloadGenerators();
        save(defaultGeneratorsConf, generatorsFile);
        update("defaultGenerators.yml", generatorsFile, Collections.emptyList());
        reloadGenerators();
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

    public List<FileConfiguration> getGeneratorsConfigurations() {
        return generatorsConfigurations;
    }

    /** Load, write back, add missing keys from the bundled resource, load again. Keeps {@code current} if nothing loads. */
    private FileConfiguration refresh(String resource, File file, FileConfiguration current) {
        FileConfiguration conf = loadOrDefault(file, resource);
        if (conf == null) conf = current;
        save(conf, file);
        update(resource, file, Collections.emptyList());
        FileConfiguration updated = loadOrDefault(file, resource);
        return updated != null ? updated : conf;
    }

    /** The file if it exists, otherwise the bundled resource; {@code null} when neither is there. */
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
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }
    }

    private void update(String resource, File file, List<String> ignoredSections) {
        try {
            ConfigUpdater.update(NewBedwars.plugin, resource, file, ignoredSections);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reloadGenerators() {
        generatorsConfigurations.clear();
        defaultGeneratorsConf = null;
        if (!generatorsFolder.exists()) {
            generatorsFolder.mkdirs();
        }
        FileConfiguration generatorsConf = null;
        if (generatorsFile.exists()) {
            generatorsConf = YamlConfiguration.loadConfiguration(generatorsFile);
            for (File file : Objects.requireNonNull(generatorsFolder.listFiles())) {
                if (file.getName().endsWith(".yml") && !file.getName().equals("defaultGenerators.yml")) {
                    generatorsConfigurations.add(YamlConfiguration.loadConfiguration(file));
                }
            }
        } else {
            generatorsConf = loadOrDefault(null, "defaultGenerators.yml");
        }
        defaultGeneratorsConf = generatorsConf;
        if (generatorsConf != null) generatorsConfigurations.add(generatorsConf);
    }

    protected final ClassLoader getMessagesClassLoader() {
        return this.classLoader;
    }
}
