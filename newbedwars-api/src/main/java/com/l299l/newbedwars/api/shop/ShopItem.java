package com.l299l.newbedwars.api.shop;

import java.util.List;
import java.util.Map;

/**
 * A shop item from items.yml. Names are case-insensitive and can't be changed once the item exists.
 *
 * @param name         letters, digits and {@code _}, 1 to 32 characters
 * @param material     a Bukkit {@code Material} name that is an item on this server
 * @param amount       1 to 64
 * @param price        0 to 100000, or {@code -1} for upgrade icons, which use the upgrade prices
 * @param priceType    one of {@link ShopOptions#priceTypes()}; {@code OTHER} is paid with {@code priceMaterial}
 * @param onBuy        what buying does, one of {@link ShopOptions#onBuyActions()}
 * @param logic        item logic key, one of {@link ShopOptions#logicKeys()}
 * @param permanent    kept after death, and unbreakable
 * @param permission   permission needed to buy, empty for none
 * @param enchantments enchantment key (e.g. {@code sharpness}) to level, 1 to 255
 * @param flags        Bukkit {@code ItemFlag} names
 * @param usedBy       where the item is used ({@code gui:<id>}, {@code config:BasicSword}). Read-only, ignored
 *                     when saving
 * @param commands       since 2.8-beta. For {@code onBuy} {@code COMMANDS}: commands run on purchase, without the leading {@code /}.
 *                       Run by the console, or by the buyer when prefixed with {@code player:}. {@code {player}},
 *                       {@code {team}} and {@code {arena}} are replaced. Up to 20 lines
 * @param openGui        since 2.8-beta. For {@code onBuy} {@code OPEN_GUI}: the id of the shop GUI to open, empty otherwise
 * @param givePermission since 2.8-beta. For {@code onBuy} {@code GIVE_PERM}: the permission the buyer gets until they leave the game
 *                       or are eliminated, empty otherwise
 * @param priceMaterial  since 2.8-beta. For {@code priceType} {@code OTHER}: the Bukkit {@code Material} name paid with, empty
 *                       otherwise
 * @since 2.7-beta
 */
public record ShopItem(String name, String material, int amount, int price, String priceType, String onBuy, String logic,
                       boolean permanent, String permission, Map<String, Integer> enchantments, List<String> flags,
                       List<String> usedBy, List<String> commands, String openGui, String givePermission,
                       String priceMaterial) {
    public ShopItem {
        enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
        flags = flags == null ? List.of() : List.copyOf(flags);
        usedBy = usedBy == null ? List.of() : List.copyOf(usedBy);
        permission = permission == null ? "" : permission;
        commands = commands == null ? List.of() : List.copyOf(commands);
        openGui = openGui == null ? "" : openGui;
        givePermission = givePermission == null ? "" : givePermission;
        priceMaterial = priceMaterial == null ? "" : priceMaterial;
    }

    /**
     * An item without commands, GUI, granted permission or price material.
     *
     * @since 2.7-beta
     */
    public ShopItem(String name, String material, int amount, int price, String priceType, String onBuy, String logic,
                    boolean permanent, String permission, Map<String, Integer> enchantments, List<String> flags,
                    List<String> usedBy) {
        this(name, material, amount, price, priceType, onBuy, logic, permanent, permission, enchantments, flags, usedBy,
                null, null, null, null);
    }

    /**
     * A plain item: costs {@code price} {@code priceType} ({@code IRON}, {@code GOLD}, {@code DIAMOND} or
     * {@code EMERALD}), gives {@code amount} {@code material} and uses the item logic {@code logic}
     * ({@code "NONE"} for none). Not permanent, with no permission, enchantments or flags.
     */
    public static ShopItem of(String name, String material, int amount, int price, String priceType, String logic) {
        return new ShopItem(name, material, amount, price, priceType, "GIVE_ITEM", logic, false, "", null, null, null);
    }
}
