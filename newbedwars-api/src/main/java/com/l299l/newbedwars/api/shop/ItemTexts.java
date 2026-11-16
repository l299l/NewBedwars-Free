package com.l299l.newbedwars.api.shop;

/**
 * The texts of a shop item in one language. {@code &} color codes work.
 *
 * @param name            display name. Empty uses the English name, and in English the item's name
 * @param iconDescription lore of the shop icon, lines separated by {@code ;}. Placeholders: {@code /price/},
 *                        {@code /amount/}, {@code /name/}. Empty uses the default shop icon text
 * @param lore            lore of the bought item, one line per line break. Empty for none
 * @since 2.7-beta
 */
public record ItemTexts(String name, String iconDescription, String lore) {
    public ItemTexts {
        name = name == null ? "" : name;
        iconDescription = iconDescription == null ? "" : iconDescription;
        lore = lore == null ? "" : lore;
    }
}
