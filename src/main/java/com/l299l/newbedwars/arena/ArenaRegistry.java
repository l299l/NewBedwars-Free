package com.l299l.newbedwars.arena;

import org.bukkit.World;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/** Every loaded arena, by name and by its current world. */
public final class ArenaRegistry {
    static final HashMap<String, IArena> BY_NAME = new HashMap<>();
    static final HashMap<World, IArena> BY_WORLD = new HashMap<>();

    private ArenaRegistry() {}

    /** The arena with exactly this name, or null. */
    public static IArena byName(String name) {
        return BY_NAME.get(name);
    }

    /** Looks an arena up by exact name, then ignoring case; null when there is none. */
    public static IArena find(String name) {
        if (name == null) return null;
        IArena arena = BY_NAME.get(name);
        if (arena != null) return arena;
        for (IArena a : BY_NAME.values()) {
            if (a.getArenaName().equalsIgnoreCase(name)) return a;
        }
        return null;
    }

    /** The arena played in this world, or null. */
    public static IArena byWorld(World world) {
        return BY_WORLD.get(world);
    }

    public static boolean contains(String name) {
        return BY_NAME.containsKey(name);
    }

    /** Live, unmodifiable view of all arenas; copy it before adding or removing arenas while iterating. */
    public static Collection<IArena> all() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }

    /** Live, unmodifiable view of the arena names. */
    public static Set<String> names() {
        return Collections.unmodifiableSet(BY_NAME.keySet());
    }

    /** Live, unmodifiable view of the arena worlds. */
    public static Set<World> worlds() {
        return Collections.unmodifiableSet(BY_WORLD.keySet());
    }

    public static void register(String name, IArena arena) {
        BY_NAME.put(name, arena);
    }

    public static void bindWorld(World world, IArena arena) {
        BY_WORLD.put(world, arena);
    }

    public static void unbindWorld(World world) {
        BY_WORLD.remove(world);
    }

    /** Removes an arena; {@code world} may be null. */
    public static void unregister(String name, World world) {
        BY_NAME.remove(name);
        if (world != null) BY_WORLD.remove(world);
    }

    public static void clear() {
        BY_NAME.clear();
        BY_WORLD.clear();
    }
}
