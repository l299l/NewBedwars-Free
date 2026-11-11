package com.l299l.newbedwars.api.shop;

import java.util.List;

/**
 * The values a shop editor can offer on this server.
 *
 * @param materials    item materials of this server version
 * @param enchantments enchantment keys
 * @param flags        item flag names
 * @param logicKeys    built-in and addon item logic keys
 * @param upgrades     team upgrade and trap names
 * @param priceTypes   price types ({@code IRON}, {@code GOLD}, ...)
 * @param onBuyActions what buying an item can do
 * @param languages    language codes of the language files ({@code en}, {@code pl}, ...)
 */
public record ShopOptions(List<String> materials, List<String> enchantments, List<String> flags, List<String> logicKeys,
                          List<String> upgrades, List<String> priceTypes, List<String> onBuyActions, List<String> languages) {
    public ShopOptions {
        materials = List.copyOf(materials);
        enchantments = List.copyOf(enchantments);
        flags = List.copyOf(flags);
        logicKeys = List.copyOf(logicKeys);
        upgrades = List.copyOf(upgrades);
        priceTypes = List.copyOf(priceTypes);
        onBuyActions = List.copyOf(onBuyActions);
        languages = List.copyOf(languages);
    }
}
