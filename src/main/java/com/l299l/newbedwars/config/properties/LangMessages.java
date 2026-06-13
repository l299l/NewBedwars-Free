package com.l299l.newbedwars.config.properties;


import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class LangMessages {
    private final HashMap<String, String> polish;
    private final FileConfiguration polishConf;
    private final HashMap<String, String> english;
    private final FileConfiguration englishConf;


    public LangMessages(FileConfiguration polishConf, FileConfiguration englishConf) {
        this.polishConf = polishConf;
        polish = new HashMap<String, String>();
        this.englishConf = englishConf;
        english = new HashMap<String, String>();
    }

    public void reloadMessages() {
        for (String key : englishConf.getKeys(false)) {
            if (!key.equals("CustomItemsNames")) {
                set(key);
            }
        }
        for (String key : polishConf.getKeys(false)) {
            if (!key.equals("CustomItemsNames") && !english.containsKey(key)) {
                set(key);
            }
        }
        setFromCustomItemsNames();
    }

    public void addCustomItemProperty(String name, String suffix) {
        polish.putIfAbsent(name + suffix, name);
        english.putIfAbsent(name + suffix, name);
    }
    public String getMsgPolish(String text) {
        return polish.get(text);
    }

    public String getMsgEnglish(String text) {
        return english.get(text);
    }

    private String getString(String text, FileConfiguration configuration) {
        String value = configuration.getString(text);
        if (value == null) return "";
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private void setFromCustomItemsNames() {
        ConfigurationSection configurationSectionPl = polishConf.getConfigurationSection("CustomItemsNames");
        ConfigurationSection configurationSectionEn = englishConf.getConfigurationSection("CustomItemsNames");
        if (configurationSectionPl != null) {
            configurationSectionPl.getValues(false).forEach((key, value) -> {
                polish.put(key, ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(configurationSectionPl.getString(key))));
            });
        }
        if (configurationSectionEn != null) {
            configurationSectionEn.getValues(false).forEach((key, value) -> {
                english.put(key, ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(configurationSectionEn.getString(key))));
            });
        }
    }

    private void set(String text) {
        polish.put(text, getString(text, polishConf));
        english.put(text, getString(text, englishConf));
    }
}
