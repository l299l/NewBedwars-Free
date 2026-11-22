package com.l299l.newbedwars.arena.generators.leveling;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public record CustomGeneratorSettings(String id, Material item, int amount, int spawnDelay, Material display, String name,
                                      boolean showNextSpawnTime) {

    public static Map<String, CustomGeneratorSettings> read(ConfigurationSection section, Logger log) {
        Map<String, CustomGeneratorSettings> out = new LinkedHashMap<>();
        if (section == null) return out;
        for (String id : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(id);
            if (c == null) continue;
            Material item = Material.matchMaterial(c.getString("Item", ""));
            if (item == null || !item.isItem() || item.isAir()) {
                log.warning("Custom generator " + id + " was skipped: unknown Item " + c.getString("Item"));
                continue;
            }
            Material display = Material.matchMaterial(c.getString("Display", item.name()));
            if (display == null || !display.isItem() || display.isAir()) display = item;
            int amount = Math.max(1, Math.min(64, c.getInt("Amount", 1)));
            int delay = Math.max(1, c.getInt("SpawnDelay", 30));
            String name = c.getString("Name", prettify(item));
            out.put(id.toLowerCase(Locale.ROOT), new CustomGeneratorSettings(id, item, amount, delay, display, name,
                    c.getBoolean("ShowNextSpawnTime", true)));
        }
        return out;
    }

    static String prettify(Material material) {
        StringBuilder sb = new StringBuilder();
        for (String word : material.name().toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
