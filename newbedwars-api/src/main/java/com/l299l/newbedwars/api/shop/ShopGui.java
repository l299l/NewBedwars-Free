package com.l299l.newbedwars.api.shop;

import java.util.List;
import java.util.Map;

/**
 * A shop or upgrade GUI, one file in {@code data/guis/}.
 *
 * @param id                 letters, digits, {@code _} and {@code -}, 1 to 32 characters; used by arenas
 * @param title              inventory title
 * @param size               9, 18, 27, 36, 45 or 54
 * @param headerSize         slots at the top that stay the same on every page, a multiple of 9
 * @param footerSize         slots at the bottom that stay the same on every page, a multiple of 9
 * @param closeOnTransaction deprecated and without effect. Always {@code false}, ignored when saving
 * @param fill               item used by {@code fill} slots: {@code ci:<item>} or {@code empty}
 * @param empty              item used by {@code empty} slots: {@code ci:<item>} or {@code empty}
 * @param slots              one entry per slot: {@code fill}, {@code empty}, {@code ci:<item>},
 *                           {@code cat:<category>} or {@code upg:<upgrade>}
 * @param categories         the categories
 * @param problems           references this server can't resolve. Read-only, ignored when saving
 * @param usedBy             where the GUI is used ({@code arena:<name>}, {@code config:DefaultTeamShopGui}).
 *                           Read-only, ignored when saving
 * @param upgradePrices      price overrides per upgrade, for arenas that use this GUI as their upgrade GUI.
 *                           Same shape as {@link UpgradePrice#tiers()}. When saving, {@code null} keeps the
 *                           current overrides and an empty map removes them
 * @since 2.7-beta
 */
public record ShopGui(String id, String title, int size, int headerSize, int footerSize, @Deprecated boolean closeOnTransaction,
                      String fill, String empty, List<String> slots, List<ShopCategory> categories,
                      List<String> problems, List<String> usedBy, Map<String, List<UpgradeTier>> upgradePrices) {
    public ShopGui {
        closeOnTransaction = false;
        slots = slots == null ? List.of() : List.copyOf(slots);
        categories = categories == null ? List.of() : List.copyOf(categories);
        problems = problems == null ? List.of() : List.copyOf(problems);
        usedBy = usedBy == null ? List.of() : List.copyOf(usedBy);
        upgradePrices = upgradePrices == null ? null : Map.copyOf(upgradePrices);
    }

    public ShopGui(String id, String title, int size, int headerSize, int footerSize, boolean closeOnTransaction,
                   String fill, String empty, List<String> slots, List<ShopCategory> categories,
                   List<String> problems, List<String> usedBy) {
        this(id, title, size, headerSize, footerSize, closeOnTransaction, fill, empty, slots, categories, problems, usedBy, null);
    }
}
