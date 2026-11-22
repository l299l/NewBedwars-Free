package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.config.Language;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ApiMessages {
    private final Map<Plugin, Map<Language, Map<String, String>>> byOwner = new LinkedHashMap<>();

    public synchronized void register(Plugin owner, Language language, Map<String, String> messages) {
        if (owner == null) throw new IllegalArgumentException("owner is null");
        if (language == null) throw new IllegalArgumentException("unknown language");
        Map<String, String> copy = new HashMap<>();
        if (messages != null) {
            for (Map.Entry<String, String> e : messages.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                copy.put(e.getKey(), ChatColor.translateAlternateColorCodes('&', e.getValue()));
            }
        }
        byOwner.computeIfAbsent(owner, o -> new EnumMap<>(Language.class)).put(language, copy);
    }

    public synchronized void release(Plugin owner) {
        byOwner.remove(owner);
    }

    public synchronized String find(Language language, String key) {
        if (key == null) return null;
        List<Map<Language, Map<String, String>>> owners = new ArrayList<>(byOwner.values());
        for (int i = owners.size() - 1; i >= 0; i--) {
            String value = lookup(owners.get(i), language, key);
            if (value != null) return value;
        }
        for (int i = owners.size() - 1; i >= 0; i--) {
            String value = lookup(owners.get(i), Language.English, key);
            if (value != null) return value;
        }
        return null;
    }

    private static String lookup(Map<Language, Map<String, String>> languages, Language language, String key) {
        Map<String, String> messages = languages.get(language);
        String value = messages == null ? null : messages.get(key);
        return value == null || value.isEmpty() ? null : value;
    }
}
