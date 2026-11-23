package com.l299l.newbedwars.api.scoreboard;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * The sidebar scoreboards of {@code scoreboards.yml}: which ones exist, which one a player sees, per-player
 * overrides and hiding. Get it with {@link com.l299l.newbedwars.api.NewBedwarsAPI#getScoreboards()}.
 * <p>
 * Call the methods on the server thread.
 *
 * @since 2.8-beta
 */
public interface ScoreboardService {
    /** Ids of the scoreboards in {@code scoreboards.yml}, sorted. */
    List<String> getScoreboards();

    /** Whether the scoreboard exists and has {@code Enabled: true}. */
    boolean isEnabled(String id);

    /**
     * The id of the scoreboard NewBedwars shows to the player right now, or {@code null} when there is none (the
     * player is not in an arena, the scoreboard is hidden or disabled).
     */
    String getCurrentScoreboard(Player player);

    /**
     * Makes the player see another scoreboard instead of the one the arena state would pick. It applies inside
     * arenas only, and is removed again when the player leaves the arena, quits, or
     * {@link #clearPlayerScoreboard(Player)} is called. A scoreboard that has {@code Enabled: false} shows nothing.
     *
     * @throws IllegalArgumentException for an id that is not in {@code scoreboards.yml}
     */
    void setPlayerScoreboard(Player player, String id);

    /** Removes the scoreboard set with {@link #setPlayerScoreboard(Player, String)}. */
    void clearPlayerScoreboard(Player player);

    /**
     * Removes the NewBedwars sidebar for the player until {@link #show(Player)} is called or the player leaves
     * the arena. Meant for addons that draw their own sidebar.
     */
    void hide(Player player);

    /** Undoes {@link #hide(Player)}. */
    void show(Player player);

    /** Renders the player's scoreboard now and sends it if it changed. */
    void refresh(Player player);
}
