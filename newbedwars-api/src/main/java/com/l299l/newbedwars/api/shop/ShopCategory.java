package com.l299l.newbedwars.api.shop;

import java.util.List;

/**
 * A category of a shop GUI: a button that opens a page with its items.
 *
 * @param id          letters, digits, {@code _} and {@code -}; {@code home} is the start page
 * @param name        button name, {@code &} color codes work
 * @param icon        name of the shop item shown as the button
 * @param description button lore, lines separated by {@code ;}
 * @param items       item references: an item name, {@code ci:<item>} or {@code upg:<upgrade>}
 * @since 2.7-beta
 */
public record ShopCategory(String id, String name, String icon, String description, List<String> items) {
    public ShopCategory {
        description = description == null ? "" : description;
        items = items == null ? List.of() : List.copyOf(items);
    }
}
