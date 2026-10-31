package com.l299l.newbedwars.arena.player.inventory;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An armor tier of the shop (leather, gold, iron, ...). The built-in tiers are registered here; addons add
 * their own (for example copper). A higher {@link #rank()} replaces a lower one; buying a lower or equal tier
 * is refused.
 */
public final class ArmorTier {
    private static final Map<String, ArmorTier> TIERS = new LinkedHashMap<>();

    public static final ArmorTier BASIC = builtIn("BASIC", 0, Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET);
    public static final ArmorTier GOLD = builtIn("GOLD", 1, Material.GOLDEN_BOOTS, Material.GOLDEN_LEGGINGS, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_HELMET);
    public static final ArmorTier CHAINMAIL = builtIn("CHAINMAIL", 2, Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET);
    public static final ArmorTier IRON = builtIn("IRON", 3, Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET);
    public static final ArmorTier DIAMOND = builtIn("DIAMOND", 4, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
    public static final ArmorTier NETHERITE = builtIn("NETHERITE", 5, Material.NETHERITE_BOOTS, Material.NETHERITE_LEGGINGS, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_HELMET);
    /** Armor made of shop items chosen by the arena ({@code otherArmorContents}); not a real tier. */
    public static final ArmorTier OTHER = new ArmorTier("OTHER", Integer.MAX_VALUE, null, null, null, null);

    private final String name;
    private final int rank;
    private final Material boots;
    private final Material leggings;
    private final Material chestplate;
    private final Material helmet;

    private ArmorTier(String name, int rank, Material boots, Material leggings, Material chestplate, Material helmet) {
        this.name = name;
        this.rank = rank;
        this.boots = boots;
        this.leggings = leggings;
        this.chestplate = chestplate;
        this.helmet = helmet;
    }

    private static ArmorTier builtIn(String name, int rank, Material boots, Material leggings, Material chestplate, Material helmet) {
        ArmorTier tier = new ArmorTier(name, rank, boots, leggings, chestplate, helmet);
        TIERS.put(name, tier);
        return tier;
    }

    /** @throws IllegalArgumentException when the name is taken or a material is not armor of the right slot */
    public static synchronized ArmorTier register(String name, int rank, Material boots, Material leggings, Material chestplate, Material helmet) {
        String key = name.trim().toUpperCase(Locale.ROOT);
        if (key.isEmpty() || TIERS.containsKey(key) || key.equals("OTHER")) {
            throw new IllegalArgumentException("armor tier '" + key + "' already exists or is invalid");
        }
        requireSlot(boots, "_BOOTS");
        requireSlot(leggings, "_LEGGINGS");
        requireSlot(chestplate, "_CHESTPLATE");
        requireSlot(helmet, "_HELMET");
        ArmorTier tier = new ArmorTier(key, rank, boots, leggings, chestplate, helmet);
        TIERS.put(key, tier);
        return tier;
    }

    private static void requireSlot(Material material, String suffix) {
        if (material == null || !material.name().endsWith(suffix)) {
            throw new IllegalArgumentException(material + " is not a " + suffix.substring(1).toLowerCase() + " piece");
        }
    }

    /** Removes an addon tier. The built-in tiers can not be removed. */
    public static synchronized boolean unregister(ArmorTier tier) {
        if (tier == null || tier == OTHER || tier == BASIC || tier == GOLD || tier == CHAINMAIL || tier == IRON
                || tier == DIAMOND || tier == NETHERITE) {
            return false;
        }
        return TIERS.remove(tier.name, tier);
    }

    public static synchronized List<ArmorTier> values() {
        return Collections.unmodifiableList(new ArrayList<>(TIERS.values()));
    }

    /** The tier a shop armor piece belongs to, or {@link #OTHER} when no tier contains the material. */
    public static synchronized ArmorTier getArmorTier(Material material) {
        for (ArmorTier tier : TIERS.values()) {
            if (tier.boots == material || tier.leggings == material || tier.chestplate == material || tier.helmet == material) {
                return tier;
            }
        }
        return OTHER;
    }

    public String name() {
        return name;
    }

    public int rank() {
        return rank;
    }

    public Material boots() {
        return boots;
    }

    public Material leggings() {
        return leggings;
    }

    public Material chestplate() {
        return chestplate;
    }

    public Material helmet() {
        return helmet;
    }

    @Override
    public String toString() {
        return name;
    }
}
