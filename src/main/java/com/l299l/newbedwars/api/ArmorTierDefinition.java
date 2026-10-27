package com.l299l.newbedwars.api;

import org.bukkit.Material;

/**
 * An armor tier for the shop (like copper). A shop item with {@code onBuyEvent: GIVE_ARMOR} and a piece of
 * this tier gives the buyer the tier; a higher {@link #getRank()} replaces a lower one. Built-in ranks:
 * leather 0, gold 1, chainmail 2, iron 3, diamond 4, netherite 5.
 */
public final class ArmorTierDefinition {
    private final String name;
    private final int rank;
    private final Material boots;
    private final Material leggings;
    private final Material chestplate;
    private final Material helmet;

    public ArmorTierDefinition(String name, int rank, Material boots, Material leggings, Material chestplate, Material helmet) {
        this.name = name;
        this.rank = rank;
        this.boots = boots;
        this.leggings = leggings;
        this.chestplate = chestplate;
        this.helmet = helmet;
    }

    public String getName() {
        return name;
    }

    public int getRank() {
        return rank;
    }

    public Material getBoots() {
        return boots;
    }

    public Material getLeggings() {
        return leggings;
    }

    public Material getChestplate() {
        return chestplate;
    }

    public Material getHelmet() {
        return helmet;
    }
}
