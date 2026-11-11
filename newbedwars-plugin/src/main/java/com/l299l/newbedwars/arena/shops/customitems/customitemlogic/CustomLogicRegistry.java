package com.l299l.newbedwars.arena.shops.customitems.customitemlogic;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class CustomLogicRegistry {
    private static final Map<String, Supplier<CustomLogic>> LOGIC = new ConcurrentHashMap<>();

    private CustomLogicRegistry() {}

    public static String normalize(String key) {
        return key.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isBuiltIn(String key) {
        try {
            LogicType.valueOf(normalize(key));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void register(String key, Supplier<CustomLogic> logic) {
        String k = normalize(key);
        if (isBuiltIn(k)) throw new IllegalArgumentException("'" + k + "' is a built-in shop logic");
        if (LOGIC.putIfAbsent(k, logic) != null) throw new IllegalArgumentException("shop logic '" + k + "' already exists");
    }

    public static boolean unregister(String key) {
        return LOGIC.remove(normalize(key)) != null;
    }

    public static java.util.List<String> keys() {
        return LOGIC.keySet().stream().sorted().toList();
    }

    public static CustomLogic create(String key) {
        Supplier<CustomLogic> supplier = LOGIC.get(normalize(key));
        return supplier == null ? null : supplier.get();
    }
}
