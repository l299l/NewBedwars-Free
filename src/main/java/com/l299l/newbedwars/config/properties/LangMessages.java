package com.l299l.newbedwars.config.properties;

import com.l299l.newbedwars.config.Language;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LangMessages {
    private final Map<Language, HashMap<String, String>> messages = new EnumMap<>(Language.class);
    private final Map<Language, FileConfiguration> configs;

    public LangMessages(Map<Language, FileConfiguration> configs) {
        this.configs = configs;
        for (Language lang : Language.values()) {
            messages.put(lang, new HashMap<>());
        }
    }

    /** Backward-compatible constructor used by tests and legacy code. */
    public LangMessages(FileConfiguration polishConf, FileConfiguration englishConf) {
        Map<Language, FileConfiguration> map = new EnumMap<>(Language.class);
        map.put(Language.English, englishConf);
        map.put(Language.Polish, polishConf);
        this.configs = map;
        for (Language lang : Language.values()) {
            messages.put(lang, new HashMap<>());
        }
    }

    public void reloadMessages() {
        for (Language lang : Language.values()) {
            messages.get(lang).clear();
        }

        // Load flat keys from each language's config
        for (Language lang : Language.values()) {
            FileConfiguration conf = configs.get(lang);
            if (conf == null) continue;
            for (String key : conf.getKeys(false)) {
                if (!key.equals("CustomItemsNames")) {
                    messages.get(lang).put(key, getString(key, conf));
                }
            }
        }

        setFromCustomItemsNames();
    }

    /**
     * Returns the message for the given language, falling back to English if the
     * key is absent in the requested language. Returns "" if not found anywhere.
     */
    public String getMsg(Language lang, String text) {
        HashMap<String, String> map = messages.get(lang);
        String result = map != null ? map.get(text) : null;
        if (result == null || result.isEmpty()) {
            HashMap<String, String> english = messages.get(Language.English);
            String fallback = english != null ? english.get(text) : null;
            if (fallback != null) return fallback;
        }
        return result != null ? result : "";
    }

    /** Returns null for unknown keys (backward compat). */
    public String getMsgEnglish(String text) {
        HashMap<String, String> map = messages.get(Language.English);
        return map != null ? map.get(text) : null;
    }

    /** Returns null for unknown keys (backward compat). */
    public String getMsgPolish(String text) {
        HashMap<String, String> map = messages.get(Language.Polish);
        return map != null ? map.get(text) : null;
    }

    public void addCustomItemProperty(String name, String suffix) {
        for (Language lang : Language.values()) {
            HashMap<String, String> map = messages.get(lang);
            if (map != null) {
                map.putIfAbsent(name + suffix, name);
            }
        }
    }

    private String getString(String text, FileConfiguration configuration) {
        String value = configuration.getString(text);
        if (value == null) return "";
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private void setFromCustomItemsNames() {
        for (Language lang : Language.values()) {
            FileConfiguration conf = configs.get(lang);
            if (conf == null) continue;
            ConfigurationSection section = conf.getConfigurationSection("CustomItemsNames");
            if (section == null) continue;
            HashMap<String, String> langMap = messages.get(lang);
            section.getValues(false).forEach((key, value) ->
                    langMap.put(key, ChatColor.translateAlternateColorCodes('&',
                            Objects.requireNonNull(section.getString(key)))));
        }
    }
}
