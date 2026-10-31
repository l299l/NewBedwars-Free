package com.l299l.newbedwars.api.registry;

import org.bukkit.Material;

/**
 * An armor tier for the shop (like copper). A shop item with {@code onBuyEvent: GIVE_ARMOR} and a piece of
 * this tier gives the buyer the tier; a higher {@link #rank()} replaces a lower one. Built-in ranks:
 * leather 0, gold 1, chainmail 2, iron 3, diamond 4, netherite 5.
 */
public record ArmorTierDefinition(String name, int rank, Material boots, Material leggings, Material chestplate, Material helmet) {
}
