package com.l299l.newbedwars.api.shop;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Reads and changes shop items (items.yml), their texts (language files) and shop GUIs ({@code data/guis/}).
 * Get it with {@code NewBedwarsAPI.getShop()}.
 * <p>
 * Every method can be called from any thread. The work runs on the server thread and the futures complete
 * there. Changes apply at once; players see them the next time they open a shop. The previous version of
 * every changed file is backed up.
 *
 * @since 2.7-beta
 */
public interface ShopEditor {
    /** All items in file order, without the ones this server couldn't load. */
    CompletableFuture<List<ShopItem>> getItems();

    CompletableFuture<Optional<ShopItem>> getItem(String name);

    /** Creates the item if the name is new, otherwise changes it. {@link ShopItem#usedBy()} is ignored. */
    CompletableFuture<ShopResult> saveItem(ShopItem item);

    /** Refused with {@link ShopResult.Code#IN_USE} while a GUI or config.yml uses the item. */
    CompletableFuture<ShopResult> deleteItem(String name);

    /** The item's texts per language code, one entry for every language in {@link ShopOptions#languages()}. */
    CompletableFuture<Optional<Map<String, ItemTexts>>> getItemTexts(String name);

    /** Sets the item's texts for the given languages. Other languages are left as they are. */
    CompletableFuture<ShopResult> saveItemTexts(String name, Map<String, ItemTexts> texts);

    CompletableFuture<List<ShopGui>> getGuis();

    CompletableFuture<Optional<ShopGui>> getGui(String id);

    /** {@link ShopResult.Code#NAME_TAKEN} when a GUI with that id, or its file, already exists. */
    CompletableFuture<ShopResult> createGui(ShopGui gui);

    /**
     * Changes an existing GUI. References to unknown items, categories or upgrades are
     * {@link ShopResult.Code#INVALID}, except ones the GUI already had (e.g. an item from a newer Minecraft
     * version), which may stay.
     */
    CompletableFuture<ShopResult> saveGui(ShopGui gui);

    /** Refused with {@link ShopResult.Code#IN_USE} while an arena or config.yml uses the GUI. */
    CompletableFuture<ShopResult> deleteGui(String id);

    /** The shop GUIs of every arena on this server. */
    CompletableFuture<List<ArenaShops>> getArenaShops();

    /**
     * Sets which GUIs an arena's shops open. Pass {@code null} to keep one unchanged. Refused with
     * {@link ShopResult.Code#BUSY} unless the arena is waiting, and with {@link ShopResult.Code#INVALID} for an
     * unknown GUI.
     */
    CompletableFuture<ShopResult> setArenaShops(String arena, String shopGui, String upgradeGui);

    /** The values an editor can offer: materials, enchantments, logic keys and so on. */
    CompletableFuture<ShopOptions> getOptions();

    /** The server-wide prices of every upgrade and trap. */
    CompletableFuture<List<UpgradePrice>> getUpgradePrices();

    /**
     * Replaces the server-wide prices of one upgrade. {@link ShopResult.Code#NOT_FOUND} for an unknown upgrade,
     * {@link ShopResult.Code#INVALID} for a negative price, an unknown currency or more tiers than
     * {@link UpgradePrice#maxLevel()}.
     */
    CompletableFuture<ShopResult> saveUpgradePrices(String upgrade, List<UpgradeTier> tiers);
}
