package com.l299l.newbedwars.api.shop;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Reads and changes shop items ({@code items.yml}), their texts (language files) and shop GUIs ({@code data/guis/}).
 * Get it with {@code NewBedwarsAPI.getShop()}. Every method can be called from any thread; the work runs on the
 * server thread. Changes apply at once: players see them the next time they open a shop. Files are written
 * atomically, the previous version is kept in {@code data/backups/shop/}. Available from 2.7-beta.
 */
public interface ShopEditor {
    /** All items in file order. Items that could not be loaded on this server are not included. */
    CompletableFuture<List<ShopItem>> getItems();

    CompletableFuture<Optional<ShopItem>> getItem(String name);

    /** Creates an item (when the name is new) or changes it; {@link ShopItem#usedBy()} is ignored. */
    CompletableFuture<ShopResult> saveItem(ShopItem item);

    /** Deletes an item; refused with {@link ShopResult.Code#IN_USE} while a GUI or the config uses it. */
    CompletableFuture<ShopResult> deleteItem(String name);

    /** The texts of an item per language code; every language of {@link ShopOptions#languages()} is included. */
    CompletableFuture<Optional<Map<String, ItemTexts>>> getItemTexts(String name);

    /** Sets the texts of an item for the given languages (other languages are not changed). */
    CompletableFuture<ShopResult> saveItemTexts(String name, Map<String, ItemTexts> texts);

    /** All GUIs. */
    CompletableFuture<List<ShopGui>> getGuis();

    CompletableFuture<Optional<ShopGui>> getGui(String id);

    /** Creates a GUI; {@link ShopResult.Code#NAME_TAKEN} when the id or its file exists. */
    CompletableFuture<ShopResult> createGui(ShopGui gui);

    /** Changes an existing GUI. References that could not be resolved before may stay unchanged. */
    CompletableFuture<ShopResult> saveGui(ShopGui gui);

    /** Deletes a GUI; refused with {@link ShopResult.Code#IN_USE} while an arena or the config uses it. */
    CompletableFuture<ShopResult> deleteGui(String id);

    /** The shop GUIs of every arena on this server. */
    CompletableFuture<List<ArenaShops>> getArenaShops();

    /**
     * Sets which GUIs an arena's shops open ({@code null} = unchanged). Refused with {@link ShopResult.Code#BUSY}
     * unless the arena is waiting; {@link ShopResult.Code#INVALID} for an unknown GUI.
     */
    CompletableFuture<ShopResult> setArenaShops(String arena, String shopGui, String upgradeGui);

    /** Values for editors (materials, enchantments, logic keys, ...). */
    CompletableFuture<ShopOptions> getOptions();
}
