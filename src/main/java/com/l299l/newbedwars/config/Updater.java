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
import java.util.List;
import java.util.Objects;

public class Updater extends Files {
    private FileConfiguration plPLConf = null;
    private FileConfiguration enConf = null;
    private FileConfiguration bossBarsConf = null;
    private FileConfiguration scoreboardsConf = null;
    private final List<FileConfiguration> generatorsConfigurations = new ArrayList<>();
    private final ClassLoader classLoader;
    private final File plPLFile = new File("plugins/NewBedwars/language/pl_PL.yml");
    private final File enFile = new File("plugins/NewBedwars/language/en.yml");
    private final File bossBarsFile = new File("plugins/NewBedwars/bossBars.yml");
    private final File scoreboardFile = new File("plugins/NewBedwars/scoreboards.yml");
    private final File generatorsFolder = new File("plugins/NewBedwars/generators");
    private final File generatorsFile = new File("plugins/NewBedwars/generators/defaultGenerators.yml");
    private final String error = ChatColor.RED + "[NewBedwars]: " + ChatColor.DARK_RED + "The language file could not be saved!";

    public void updateConf() {
        NewBedwars.plugin.saveDefaultConfig();
        File configFile = new File(NewBedwars.plugin.getDataFolder(), "config.yml");
        try {
            ConfigUpdater.update(NewBedwars.plugin, "config.yml", configFile, Collections.emptyList());
        }catch (
                IOException e) {
            e.printStackTrace();
        }
        NewBedwars.plugin.reloadConfig();
    }

    public void reloadPolish() {
        reloadplPL();
        saveplPL();
        try {
            ConfigUpdater.update(NewBedwars.plugin, "pl_PL.yml",plPLFile, Collections.singletonList("CustomItemsNames"));
        }catch (IOException e) {
            e.printStackTrace();
        }
        reloadplPL(); }

    public void reloadEnglish() {
        reloaEn();
        saveEn();
        try {
            ConfigUpdater.update(NewBedwars.plugin, "en.yml",enFile, Collections.singletonList("CustomItemsNames"));
        }catch (IOException e) {
            e.printStackTrace();
        }
        reloaEn();
    }

    public void reloadBossBars() {
        reloaBossBars();
        saveBossBars();
        try {
            ConfigUpdater.update(NewBedwars.plugin, "bossBars.yml",bossBarsFile, Collections.emptyList());
        }catch (IOException e) {
            e.printStackTrace();
        }
        reloaBossBars();
    }

    public void reloadScoreboards() {
        reloaScoreboards();
        saveScoreboards();
        try {
            ConfigUpdater.update(NewBedwars.plugin, "scoreboards.yml", scoreboardFile, Collections.emptyList());
        }catch (IOException e) {
            e.printStackTrace();
        }
        reloaScoreboards();
    }

    public void reloadGeneratorsConfigurations() {
        reloadGenerators();
        saveGenerators();
        try {
            ConfigUpdater.update(NewBedwars.plugin, "defaultGenerators.yml", generatorsFile, Collections.emptyList());
        }catch (IOException e) {
            e.printStackTrace();
        }
        reloadGenerators();
    }

    public Updater() {
        classLoader = getClass().getClassLoader();
    }

    public FileConfiguration getPlPLConf() {
        return plPLConf;
    }

    public FileConfiguration getEnConf() {
        return enConf;
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

    public File getPlPLFile() {
        return plPLFile;
    }

    public File getEnFile() {
        return enFile;
    }

    private void reloadplPL() {
        if (plPLFile.exists()) {
            plPLConf = YamlConfiguration.loadConfiguration(plPLFile);
        } else {
            InputStream defConfigStream = getFilesResource("pl_PL.yml", getMessagesClassLoader());
            if (defConfigStream != null) {
                plPLConf = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            }
        }
    }

    private void saveplPL() {
        try {
            plPLConf.save(plPLFile);
        } catch (IOException var2) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }

    }

    private void reloaEn() {
        if (enFile.exists()) {
            enConf = YamlConfiguration.loadConfiguration(enFile);
        } else {
            InputStream defConfigStream = getFilesResource("en.yml", getMessagesClassLoader());
            if (defConfigStream != null) {
                enConf = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            }
        }
    }

    private void saveEn() {
        try {
            enConf.save(this.enFile);
        } catch (IOException var2) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }

    }

    private void reloaBossBars() {
        if (bossBarsFile.exists()) {
            bossBarsConf = YamlConfiguration.loadConfiguration(bossBarsFile);
        } else {
            InputStream defConfigStream = getFilesResource("bossBars.yml", getMessagesClassLoader());
            if (defConfigStream != null) {
                bossBarsConf = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            }
        }
    }

    private void saveBossBars() {
        try {
            bossBarsConf.save(bossBarsFile);
        } catch (IOException var2) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }

    }

    private void reloaScoreboards() {
        if (scoreboardFile.exists()) {
            scoreboardsConf = YamlConfiguration.loadConfiguration(scoreboardFile);
        } else {
            InputStream defConfigStream = getFilesResource("scoreboards.yml", getMessagesClassLoader());
            if (defConfigStream != null) {
                scoreboardsConf = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            }
        }
    }

    private void saveScoreboards() {
        try {
            scoreboardsConf.save(scoreboardFile);
        } catch (IOException var2) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }

    }

    private void reloadGenerators() {
        if (!generatorsFolder.exists()) {
            generatorsFolder.mkdirs();
        }
        FileConfiguration generatorsConf = null;
        if (generatorsFile.exists()) {
            generatorsConf = YamlConfiguration.loadConfiguration(generatorsFile);
            for (File file : Objects.requireNonNull(generatorsFolder.listFiles())) {
                if (file.getName().endsWith(".yml")) {
                    if (file.getName().equals("defaultGenerators.yml")) {
                        continue;
                    }
                    FileConfiguration conf = YamlConfiguration.loadConfiguration(file);
                    generatorsConfigurations.add(conf);
                }
            }
        } else {
            InputStream defConfigStream = getFilesResource("defaultGenerators.yml", getMessagesClassLoader());
            if (defConfigStream != null) {
                generatorsConf = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            }
        }
        if (generatorsConf != null) {
            generatorsConfigurations.add(generatorsConf);
        }
    }

    private void saveGenerators() {
        try {
            generatorsConfigurations.get(0).save(generatorsFile);
        } catch (IOException var2) {
            NewBedwars.plugin.getServer().getConsoleSender().sendMessage(error);
        }
    }

    protected final ClassLoader getMessagesClassLoader() {
        return this.classLoader;
    }



}
