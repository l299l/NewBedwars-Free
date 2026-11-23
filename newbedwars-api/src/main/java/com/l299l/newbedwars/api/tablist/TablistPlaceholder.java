package com.l299l.newbedwars.api.tablist;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;

/**
 * Supplies the text of a {@code {name}} placeholder in {@code tablists.yml}. See
 * {@link TablistService#registerPlaceholder}.
 *
 * @since 2.8-beta
 */
@FunctionalInterface
public interface TablistPlaceholder {
    /**
     * Called on the server thread every time a tab list is rendered. Keep it fast.
     *
     * @param viewer the player the tab list is for; in {@code PlayerFormat} the player whose name is being listed
     * @param arena  the arena the player plays or spectates in, or {@code null} in the lobby
     * @return the text, {@code &} color codes are translated afterwards; {@code null} counts as empty
     */
    String resolve(Player viewer, ArenaView arena);
}
