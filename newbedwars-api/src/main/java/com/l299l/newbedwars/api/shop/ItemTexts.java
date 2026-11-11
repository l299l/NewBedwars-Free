package com.l299l.newbedwars.api.shop;

/**
 * The texts of a shop item in one language ({@code CustomItemsNames} in the language file), with {@code &} colour codes.
 *
 * @param name            display name; empty = the English name (in English: the item name)
 * @param iconDescription lore of the shop icon, lines split by {@code ;}, placeholders {@code /price/},
 *                        {@code /amount/}, {@code /name/}; empty = the default shop icon text
 * @param lore            lore of the bought item, lines split by a new line; empty = none
 */
public record ItemTexts(String name, String iconDescription, String lore) {
    public ItemTexts {
        name = name == null ? "" : name;
        iconDescription = iconDescription == null ? "" : iconDescription;
        lore = lore == null ? "" : lore;
    }
}
