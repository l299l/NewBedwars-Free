package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.properties.Properties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Holds the arena groups (case-insensitive names); an arena belongs to at most one group. */
public final class ArenaGroupManager {
    private static ArenaGroupManager instance;
    private final Map<String, ArenaGroup> groups = new LinkedHashMap<>();

    public ArenaGroupManager() {
        instance = this;
    }

    /** The active manager, or null before the plugin loaded it (e.g. in unit tests). */
    public static ArenaGroupManager get() {
        return instance;
    }

    /** The group called {@code name}, or null (also when no manager is loaded). */
    public static ArenaGroup lookup(String name) {
        return instance == null || name == null ? null : instance.group(name);
    }

    public void load() {
        groups.clear();
        try {
            for (ArenaGroup g : ArenaGroupStore.load(file())) groups.put(key(g.getName()), g);
        } catch (IOException | RuntimeException e) {
            NewBedwars.plugin.getLogger().warning("Could not read data/groups.json: " + e.getMessage());
        }
    }

    public void save() {
        List<ArenaGroup> snapshot = new ArrayList<>(groups.values());
        Path file = file();
        Path backups = NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("backups").resolve("groups");
        int keep = Properties.BackupsKeep;
        IOExecutor.submit(() -> {
            try {
                ArenaGroupStore.save(file, snapshot, backups, keep);
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().warning("Could not save data/groups.json: " + e.getMessage());
            }
        });
    }

    private Path file() {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("groups.json");
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public Collection<ArenaGroup> getGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    public ArenaGroup group(String name) {
        return groups.get(key(name));
    }

    /** The group {@code arena} belongs to, or null. */
    public ArenaGroup groupOf(IArena arena) {
        return groupOf(arena.getArenaName());
    }

    public ArenaGroup groupOf(String arenaName) {
        for (ArenaGroup g : groups.values()) {
            if (g.contains(arenaName)) return g;
        }
        return null;
    }

    /** @return false when a group with that name exists */
    public boolean create(String name, String displayItem) {
        if (groups.containsKey(key(name))) return false;
        groups.put(key(name), new ArenaGroup(name, displayItem));
        save();
        return true;
    }

    /** Adds a group without saving (tests). */
    public void register(ArenaGroup group) {
        groups.put(key(group.getName()), group);
    }

    public boolean delete(String name) {
        if (groups.remove(key(name)) == null) return false;
        save();
        return true;
    }

    public void changed() {
        save();
    }

    /** Arenas of the group that exist. */
    public static List<IArena> arenas(ArenaGroup group) {
        return group.resolve(ArenaRegistry::find);
    }
}
