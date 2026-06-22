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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Updater extends Files {
    private final Map<Language, FileConfiguration> langConfs = new EnumMap<>(Language.class);
    private final Map<Language, File> langFiles = new EnumMap<>(Language.class);
    private FileConfiguration bossBarsConf = null;
    private FileConfiguration scoreboardsConf = null;
    private final List<FileConfiguration> generatorsConfigurations = new ArrayList<>();
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
            reloadLang(lang);
            saveLang(lang);
            FileConfiguration conf = langConfs.get(lang);
            List<String> ignored = conf != null && conf.contains("CustomItemsNames")
                    ? Collections.singletonList("CustomItemsNames") : Collections.emptyList();
            try {
                ConfigUpdater.update(NewBedwars.plugin, lang.getCode() + ".yml", langFiles.get(lang), ignored);
            } catch (Exception e) {
                e.printStackTrace();
            }
            reloadLang(lang);
        }
    }

    public void reloadBossBars() {
        reloadBossBarsInternal();
        saveBossBars();
        try {
            ConfigUpdater.update(NewBedwars.plugin, "bossBars.yml", bossBarsFile, Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        reloadBossBarsInternal();
    }

    public void reloadScoreboards() {
        reloadScoreboardsInternal();
        saveScoreboards();
        try {
            ConfigUpdater.update(NewBedwars.plugin, "scoreboards.yml", scoreboardFile, Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        reloadScoreboardsInternal();
    }

    public void reloadGeneratorsConfigurations() {
        reloadGenerators();
        saveGenerators();
        try {
            ConfigUpdater.update(NewBedwars.plugin, "defaultGenerators.yml", generatorsFile, Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void reloadLang(Language lang) {
        File file = langFiles.get(lang);
        if (file != null && file.exists()) {
            langConfs.put(lang, YamlConfiguration.loadConfiguration(file));
        } else {
            InputStream stream = getFilesResource(lang.getCode() + ".yml", getMessagesClassLoader());
            if (stream != null) {
                langConfs.put(lang, YamlConfiguration.loadConfiguration(new InputStreamReader(stream)));
            }
        }
    }

    private void saveLang(Language lang) {
        FileConfiguration conf = langConfs.get(lang);
        if (conf == null) return;
        File file = langFiles.get(lang);
        try {
            conf.save(file);
        } catch (IOException e) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }
    }

    private void reloadBossBarsInternal() {
        if (bossBarsFile.exists()) {
            bossBarsConf = YamlConfiguration.loadConfiguration(bossBarsFile);
        } else {
            InputStream s = getFilesResource("bossBars.yml", getMessagesClassLoader());
            if (s != null) bossBarsConf = YamlConfiguration.loadConfiguration(new InputStreamReader(s));
        }
    }

    private void saveBossBars() {
        try {
            bossBarsConf.save(bossBarsFile);
        } catch (IOException e) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }
    }

    private void reloadScoreboardsInternal() {
        if (scoreboardFile.exists()) {
            scoreboardsConf = YamlConfiguration.loadConfiguration(scoreboardFile);
        } else {
            InputStream s = getFilesResource("scoreboards.yml", getMessagesClassLoader());
            if (s != null) scoreboardsConf = YamlConfiguration.loadConfiguration(new InputStreamReader(s));
        }
    }

    private void saveScoreboards() {
        try {
            scoreboardsConf.save(scoreboardFile);
        } catch (IOException e) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }
    }

    private void reloadGenerators() {
        generatorsConfigurations.clear();
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
            InputStream s = getFilesResource("defaultGenerators.yml", getMessagesClassLoader());
            if (s != null) generatorsConf = YamlConfiguration.loadConfiguration(new InputStreamReader(s));
        }
        if (generatorsConf != null) generatorsConfigurations.add(generatorsConf);
    }

    private void saveGenerators() {
        try {
            generatorsConfigurations.get(0).save(generatorsFile);
        } catch (IOException e) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }
    }

    protected final ClassLoader getMessagesClassLoader() {
        return this.classLoader;
    }
}
