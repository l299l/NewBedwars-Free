package com.l299l.newbedwars.api.text;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;

/**
 * Supplies the text of a custom placeholder. See
 * {@link com.l299l.newbedwars.api.NewBedwarsAPI#registerPlaceholder}.
 *
 * @since 2.8-beta
 */
@FunctionalInterface
public interface TextPlaceholder {
    /**
     * Called on the server thread every time a text using the placeholder is rendered. Keep it fast.
     *
     * @param viewer       the player the text is for; in the {@code PlayerFormat} of a tab list the player whose name
     *                     is being listed. It is {@code null} for the boss bars of an arena, which all players of
     *                     the arena share, and for the lobby boss bar
     * @param arenaOrNull  the arena the text belongs to, or {@code null} in the lobby
     * @return the text, {@code &} color codes are translated afterwards; {@code null} counts as empty
     */
    String resolve(Player viewer, ArenaView arenaOrNull);
}
