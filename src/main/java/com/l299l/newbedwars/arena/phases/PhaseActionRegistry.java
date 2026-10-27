package com.l299l.newbedwars.arena.phases;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.GeneratorType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Actions a game phase can run, by name (as written under {@code GamePhases:} in config.yml). The built-in
 * actions are registered here; addons add their own through the API.
 */
public final class PhaseActionRegistry {
    private static final Map<String, BiConsumer<IArena, String>> ACTIONS = new LinkedHashMap<>();

    static {
        register("DIAMOND_GENERATOR", (arena, arg) -> arena.upgradeGenerators(GeneratorType.DIAMOND, Integer.parseInt(arg)));
        register("EMERALD_GENERATOR", (arena, arg) -> arena.upgradeGenerators(GeneratorType.EMERALD, Integer.parseInt(arg)));
        register("BED_DESTRUCTION", (arena, arg) -> arena.bedDestruction());
        register("END_GAME", (arena, arg) -> arena.endGame(arg));
        register("GAME_END", (arena, arg) -> arena.endGame(arg));
        register("ENDER_DRAGON_END", (arena, arg) -> arena.enderDragonEnd());
        register("ENDER_DRAGON_START", (arena, arg) -> arena.enderDragonStart());
        register("DISABLE_REGENERATION", (arena, arg) -> arena.disableRegeneration());
        register("DISABLE_TEAMS_FORGE", (arena, arg) -> arena.disableTeamsForge());
    }

    private PhaseActionRegistry() {}

    /** @throws IllegalArgumentException when the name is already taken */
    public static synchronized void register(String name, BiConsumer<IArena, String> action) {
        String key = normalize(name);
        if (ACTIONS.containsKey(key)) throw new IllegalArgumentException("phase action '" + key + "' already exists");
        ACTIONS.put(key, action);
    }

    public static synchronized boolean unregister(String name) {
        return ACTIONS.remove(normalize(name)) != null;
    }

    public static synchronized BiConsumer<IArena, String> get(String name) {
        return ACTIONS.get(normalize(name));
    }

    public static synchronized Collection<String> names() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(ACTIONS.keySet()));
    }

    private static String normalize(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }
}
