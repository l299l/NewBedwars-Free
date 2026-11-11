package com.l299l.newbedwars.api.shop;

import java.util.List;

/**
 * A shop or upgrade GUI (a file in {@code data/guis/}).
 *
 * @param id                 letters, digits, {@code _} and {@code -}, 1 to 32 characters; used by arenas
 * @param title              inventory title
 * @param size               9, 18, 27, 36, 45 or 54
 * @param headerSize         slots on top that stay on every page (a multiple of 9)
 * @param footerSize         slots at the bottom that stay on every page (a multiple of 9)
 * @param closeOnTransaction close the GUI after a purchase
 * @param fill               item used by {@code fill} slots: {@code ci:<item>} or {@code empty}
 * @param empty              item used by {@code empty} slots: {@code ci:<item>} or {@code empty}
 * @param slots              one entry per slot: {@code fill}, {@code empty}, {@code ci:<item>}, {@code cat:<category>}
 *                           or {@code upg:<upgrade>}
 * @param categories         the categories
 * @param problems           read-only: references that could not be resolved; ignored when saving
 * @param usedBy             read-only: where the GUI is used ({@code arena:<name>}, {@code config:DefaultTeamShopGui});
 *                           ignored when saving
 */
public record ShopGui(String id, String title, int size, int headerSize, int footerSize, boolean closeOnTransaction,
                      String fill, String empty, List<String> slots, List<ShopCategory> categories,
                      List<String> problems, List<String> usedBy) {
    public ShopGui {
        slots = slots == null ? List.of() : List.copyOf(slots);
        categories = categories == null ? List.of() : List.copyOf(categories);
        problems = problems == null ? List.of() : List.copyOf(problems);
        usedBy = usedBy == null ? List.of() : List.copyOf(usedBy);
    }
}
