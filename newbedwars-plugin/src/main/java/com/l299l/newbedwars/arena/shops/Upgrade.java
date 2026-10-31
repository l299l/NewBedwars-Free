package com.l299l.newbedwars.arena.shops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A team upgrade or trap. The built-in ones are constants; addons register more with {@link #register}.
 * Names are upper case and are used in config.yml ({@code UpgradePrices}) and the shop GUI files.
 */
public final class Upgrade {
    private static final Map<String, Upgrade> REGISTRY = new LinkedHashMap<>();

    public static final Upgrade PROTECTION = builtIn("PROTECTION", 5, BuiltInUpgrades.protection());
    public static final Upgrade SHARPNESS = builtIn("SHARPNESS", 4, BuiltInUpgrades.sharpness());
    public static final Upgrade HASTE = builtIn("HASTE", 2, BuiltInUpgrades.haste());
    public static final Upgrade FORGE = builtIn("FORGE", 8, BuiltInUpgrades.forge());
    public static final Upgrade HEALPOOL = builtIn("HEALPOOL", 1, BuiltInUpgrades.healPool());
    public static final Upgrade ALARMTRAP = builtIn("ALARMTRAP", 2, BuiltInUpgrades.alarmTrap());
    public static final Upgrade BLINDTRAP = builtIn("BLINDTRAP", 2, BuiltInUpgrades.blindTrap());
    public static final Upgrade MININGFATIGUETRAP = builtIn("MININGFATIGUETRAP", 2, BuiltInUpgrades.miningFatigueTrap());
    public static final Upgrade CUSTOMTRAP = builtIn("CUSTOMTRAP", 0, new UpgradeHandler() {});
    public static final Upgrade CUSTOMUPGRADE = builtIn("CUSTOMUPGRADE", 0, new UpgradeHandler() {});
    public static final Upgrade DRAGONBUFF = builtIn("DRAGONBUFF", 2, new UpgradeHandler() {});

    public final int maxLevel;
    private final String name;
    private final UpgradeHandler handler;
    private final List<Integer> defaultPrices;
    private final boolean builtIn;

    private Upgrade(String name, int maxLevel, UpgradeHandler handler, List<Integer> defaultPrices, boolean builtIn) {
        this.name = name;
        this.maxLevel = maxLevel;
        this.handler = handler;
        this.defaultPrices = defaultPrices;
        this.builtIn = builtIn;
    }

    private static Upgrade builtIn(String name, int maxLevel, UpgradeHandler handler) {
        Upgrade upgrade = new Upgrade(name, maxLevel, handler, List.of(), true);
        REGISTRY.put(name, upgrade);
        return upgrade;
    }

    /**
     * @param defaultPrices price per level, used when config.yml has no {@code UpgradePrices.<NAME>} list
     * @throws IllegalArgumentException when the name is taken
     */
    public static synchronized Upgrade register(String name, int maxLevel, UpgradeHandler handler, List<Integer> defaultPrices) {
        String key = name.trim().toUpperCase(Locale.ROOT);
        if (key.isEmpty() || REGISTRY.containsKey(key)) throw new IllegalArgumentException("upgrade '" + key + "' already exists or is invalid");
        Upgrade upgrade = new Upgrade(key, maxLevel, handler, List.copyOf(defaultPrices), false);
        REGISTRY.put(key, upgrade);
        return upgrade;
    }

    /** Removes an addon upgrade; the built-in ones stay. */
    public static synchronized boolean unregister(Upgrade upgrade) {
        return upgrade != null && !upgrade.builtIn && REGISTRY.remove(upgrade.name, upgrade);
    }

    public static synchronized List<Upgrade> values() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY.values()));
    }

    /** @throws IllegalArgumentException when there is no upgrade with that name */
    public static synchronized Upgrade valueOf(String name) {
        Upgrade upgrade = REGISTRY.get(name.trim().toUpperCase(Locale.ROOT));
        if (upgrade == null) throw new IllegalArgumentException("No upgrade named " + name);
        return upgrade;
    }

    public String name() {
        return name;
    }

    public UpgradeHandler handler() {
        return handler;
    }

    public List<Integer> defaultPrices() {
        return defaultPrices;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    @Override
    public String toString() {
        return name;
    }
}
