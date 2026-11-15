package com.l299l.newbedwars.api.shop;

import java.util.List;
import java.util.Map;

/**
 * A shop item as stored in {@code items.yml}. Names are case-insensitive and can not be changed after creation.
 *
 * @param name         letters, digits and {@code _}, 1 to 32 characters
 * @param material     a Bukkit {@code Material} name that is an item on this server
 * @param amount       1 to 64
 * @param price        0 to 100000 ({@code -1} for upgrade icons, whose price comes from {@code data/upgrades.yml})
 * @param priceType    one of {@link ShopOptions#priceTypes()}
 * @param onBuy        what buying does, one of {@link ShopOptions#onBuyActions()}
 * @param logic        item logic key, one of {@link ShopOptions#logicKeys()}
 * @param permanent    kept after death (and unbreakable)
 * @param permission   permission needed to buy, empty for none
 * @param enchantments enchantment key (e.g. {@code sharpness}) to level 1..255
 * @param flags        Bukkit {@code ItemFlag} names
 * @param usedBy       read-only: where the item is used ({@code gui:<id>}, {@code config:BasicSword}); ignored when saving
 */
public record ShopItem(String name, String material, int amount, int price, String priceType, String onBuy, String logic,
                       boolean permanent, String permission, Map<String, Integer> enchantments, List<String> flags,
                       List<String> usedBy) {
    public ShopItem {
        enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
        flags = flags == null ? List.of() : List.copyOf(flags);
        usedBy = usedBy == null ? List.of() : List.copyOf(usedBy);
        permission = permission == null ? "" : permission;
    }

    /**
     * A plain item sold for {@code price} of {@code priceType} ({@code IRON}, {@code GOLD}, {@code DIAMOND} or
     * {@code EMERALD}) that gives {@code amount} of {@code material} and runs the item logic {@code logic}
     * ({@code "NONE"} for no logic). Not permanent, no permission, enchantments or flags.
     */
    public static ShopItem of(String name, String material, int amount, int price, String priceType, String logic) {
        return new ShopItem(name, material, amount, price, priceType, "GIVE_ITEM", logic, false, "", null, null, null);
    }
}
