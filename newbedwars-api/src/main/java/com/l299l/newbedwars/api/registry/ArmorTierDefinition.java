package com.l299l.newbedwars.api.registry;

import org.bukkit.Material;

/**
 * A custom armor tier. A shop item with {@code onBuyEvent: GIVE_ARMOR} whose material is one of this tier's
 * pieces gives the buyer the whole tier. Buying a tier with a lower rank than the current one does nothing.
 *
 * @param name       unique name of the tier
 * @param rank       built-in ranks are leather 0, gold 1, chainmail 2, iron 3, diamond 4 and netherite 5
 * @param boots      material of the boots
 * @param leggings   material of the leggings
 * @param chestplate material of the chestplate
 * @param helmet     material of the helmet
 */
public record ArmorTierDefinition(String name, int rank, Material boots, Material leggings, Material chestplate, Material helmet) {
}
