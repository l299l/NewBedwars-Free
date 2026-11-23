package com.l299l.newbedwars.tablist;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class TablistConfig {
    public static final String DEFAULT_ID = "Default";

    private final boolean lobbyEnabled;
    private final TablistSection lobby;
    private final Map<String, TablistLayout> layouts;

    private TablistConfig(boolean lobbyEnabled, TablistSection lobby, Map<String, TablistLayout> layouts) {
        this.lobbyEnabled = lobbyEnabled;
        this.lobby = lobby;
        this.layouts = layouts;
    }

    public static TablistConfig empty() {
        return new TablistConfig(false, TablistSection.EMPTY, new LinkedHashMap<>());
    }

    public static TablistConfig parse(ConfigurationSection root, Logger log) {
        if (root == null) return empty();
        ConfigurationSection lobbySection = root.getConfigurationSection("Lobby");
        boolean lobbyEnabled = lobbySection != null && lobbySection.getBoolean("Enabled", false);
        TablistSection lobby = section(lobbySection);
        Map<String, TablistLayout> layouts = new LinkedHashMap<>();
        ConfigurationSection layoutsSection = root.getConfigurationSection("Layouts");
        if (layoutsSection != null) {
            for (String id : layoutsSection.getKeys(false)) {
                ConfigurationSection layout = layoutsSection.getConfigurationSection(id);
                if (layout == null) {
                    if (log != null) log.warning("tablists.yml: layout '" + id + "' is not a section and was skipped");
                    continue;
                }
                layouts.put(id, new TablistLayout(id, section(layout.getConfigurationSection("Waiting")),
                        section(layout.getConfigurationSection("Playing")),
                        section(layout.getConfigurationSection("Spectator"))));
            }
        }
        return new TablistConfig(lobbyEnabled, lobby, layouts);
    }

    private static TablistSection section(ConfigurationSection section) {
        if (section == null) return TablistSection.EMPTY;
        return new TablistSection(lines(section, "Header"), lines(section, "Footer"),
                section.get("PlayerFormat") instanceof String s ? s : null);
    }

    private static List<String> lines(ConfigurationSection section, String key) {
        if (!section.contains(key)) return null;
        if (section.isList(key)) return section.getStringList(key);
        String single = section.getString(key);
        return single == null || single.isEmpty() ? List.of() : List.of(single);
    }

    public boolean isLobbyEnabled() {
        return lobbyEnabled;
    }

    public TablistSection lobby() {
        return lobby;
    }

    public Map<String, TablistLayout> layouts() {
        return layouts;
    }

    public TablistLayout find(String id) {
        if (id == null) return null;
        TablistLayout exact = layouts.get(id);
        if (exact != null) return exact;
        for (Map.Entry<String, TablistLayout> e : layouts.entrySet()) {
            if (e.getKey().equalsIgnoreCase(id)) return e.getValue();
        }
        return null;
    }
}
