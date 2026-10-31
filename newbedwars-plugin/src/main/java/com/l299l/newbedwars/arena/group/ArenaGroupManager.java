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

/**
 * Holds the arena groups (case-insensitive names); an arena belongs to at most one group.
 * In network mode lobby servers define the groups ({@link #getRevision()} = time of the last edit, the newest set
 * wins) and game servers only use the network's groups for their own arenas ({@link #useNetwork}, never saved).
 */
public final class ArenaGroupManager {
    private static ArenaGroupManager instance;
    private final Map<String, ArenaGroup> groups = new LinkedHashMap<>();
    private long revision;
    private boolean networkManaged;
    private Runnable changeListener;

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
        revision = 0;
        networkManaged = false;
        try {
            ArenaGroupStore.Loaded loaded = ArenaGroupStore.read(file());
            for (ArenaGroup g : loaded.groups()) groups.put(key(g.getName()), g);
            revision = loaded.revision();
        } catch (IOException | RuntimeException e) {
            NewBedwars.plugin.getLogger().warning("Could not read data/groups.json: " + e.getMessage());
        }
    }

    public void save() {
        if (networkManaged) return;
        List<ArenaGroup> snapshot = new ArrayList<>(groups.values());
        long rev = revision;
        Path file = file();
        Path backups = NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("backups").resolve("groups");
        int keep = Properties.BackupsKeep;
        IOExecutor.submit(() -> {
            try {
                ArenaGroupStore.save(file, snapshot, rev, backups, keep);
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
        edited();
        return true;
    }

    /** Adds a group without saving (tests). */
    public void register(ArenaGroup group) {
        groups.put(key(group.getName()), group);
    }

    public boolean delete(String name) {
        if (groups.remove(key(name)) == null) return false;
        edited();
        return true;
    }

    public void changed() {
        edited();
    }

    private void edited() {
        if (networkManaged) return;
        revision = Math.max(System.currentTimeMillis(), revision + 1);
        save();
        if (changeListener != null) changeListener.run();
    }

    /** Time of the last edit (0 = never edited); used to find the newest groups of a network. */
    public long getRevision() {
        return revision;
    }

    /** Called after every edit (network lobby: send the groups to the proxy); null to remove. */
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    /** Game server in network mode: the groups come from the lobby and are not edited or saved here. */
    public boolean isNetworkManaged() {
        return networkManaged;
    }

    /** Network lobby: take newer groups edited on another lobby and save them. */
    public void replace(List<ArenaGroup> newGroups, long newRevision) {
        groups.clear();
        for (ArenaGroup g : newGroups) groups.put(key(g.getName()), g);
        revision = newRevision;
        save();
    }

    /** Network game server: use these groups (already reduced to this server's arenas); nothing is saved. */
    public void useNetwork(List<ArenaGroup> newGroups) {
        networkManaged = true;
        groups.clear();
        for (ArenaGroup g : newGroups) groups.putIfAbsent(key(g.getName()), g);
    }

    /** Arenas of the group that exist. */
    public static List<IArena> arenas(ArenaGroup group) {
        return group.resolve(ArenaRegistry::find);
    }
}
