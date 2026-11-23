package com.l299l.newbedwars.api.tablist;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * The custom tab list ({@code tablists.yml}): which layout applies, your own placeholders and per-player
 * layouts. Get it with {@link com.l299l.newbedwars.api.NewBedwarsAPI#getTablists()}.
 * <p>
 * Call the methods on the server thread.
 *
 * @since 2.8-beta
 */
public interface TablistService {
    /** Whether the tab list feature is on ({@code Tablist.Enabled} in config.yml). */
    boolean isEnabled();

    /** Ids of the layouts in {@code tablists.yml}, sorted. */
    List<String> getLayouts();

    /** The layout arenas use when they have none of their own ({@code DefaultTablist} in config.yml). */
    String getDefaultLayout();

    /**
     * The layout players of {@code arena} get when nothing overrides it per player: the arena's own layout
     * ({@link ArenaView#getTablist()}), else {@link #getDefaultLayout()}; a layout that doesn't exist falls back to
     * {@code Default}.
     *
     * @param arena the arena, or {@code null} for the default
     */
    String getEffectiveLayout(ArenaView arena);

    /** Whether the {@code Lobby} section of {@code tablists.yml} is on, i.e. players outside arenas get a tab list. */
    boolean isLobbyEnabled();

    /**
     * Adds a {@code {name}} placeholder for the Header, Footer and PlayerFormat lines of {@code tablists.yml}. It is
     * resolved before {@code &} color codes and PlaceholderAPI are applied. In {@code PlayerFormat} the viewer
     * argument is the player being listed. A placeholder that throws is logged once and renders as empty. It is
     * removed when {@code owner} is disabled or {@link com.l299l.newbedwars.api.NewBedwarsAPI#unregisterAll}
     * is called.
     *
     * @param name letters, digits and {@code _}, 1 to 32 characters, without the braces
     * @throws IllegalArgumentException for an invalid name, a name NewBedwars already uses (such as {@code player}
     *                                  or {@code arena}) and a name that is already registered
     */
    void registerPlaceholder(Plugin owner, String name, TablistPlaceholder placeholder);

    /**
     * Makes {@code player} see another layout, which wins over the arena's and the default one. It applies inside
     * arenas only, and is removed again when the player leaves the arena, quits, or
     * {@link #clearPlayerLayout(Player)} is called.
     *
     * @throws IllegalArgumentException for a layout that doesn't exist
     */
    void setPlayerLayout(Player player, String layoutId);

    /** Removes the layout set with {@link #setPlayerLayout(Player, String)}. */
    void clearPlayerLayout(Player player);

    /** Renders the player's tab list now, and sends it if it changed. */
    void refresh(Player player);

    /** {@link #refresh(Player)} for everyone who plays or spectates in the arena. */
    void refresh(ArenaView arena);
}
