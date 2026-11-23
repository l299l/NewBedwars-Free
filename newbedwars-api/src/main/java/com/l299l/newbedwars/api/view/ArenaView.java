package com.l299l.newbedwars.api.view;

import com.l299l.newbedwars.api.edit.ArenaEditor;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only view of an arena. Get one from {@link NewBedwarsAPI}. */
public interface ArenaView {
    String getName();

    ArenaState getState();

    boolean isEnabled();

    /** The arena's world. It is replaced after every map reset, so don't keep a reference to it. */
    World getWorld();

    int getMinPlayers();

    int getMaxInTeam();

    /** Players taking part, without spectators. */
    List<Player> getPlayers();

    List<Player> getSpectators();

    List<TeamView> getTeams();

    Optional<TeamView> getTeam(String name);

    Optional<GamePlayerView> getGamePlayer(UUID player);

    /** The current phase's name, or {@code null} when no game is running. */
    String getCurrentPhase();

    /** Seconds since the game started, 0 when no game is running. */
    int getGameTime();

    /**
     * The next phase's name, or {@code null} when there is none.
     *
     * @since 2.7-beta
     */
    String getNextPhase();

    /**
     * Seconds until the current phase ends, or -1 when no phase timer runs.
     *
     * @since 2.7-beta
     */
    int getPhaseTimeLeft();

    /**
     * Id of the GUI the item shop opens.
     *
     * @since 2.7-beta
     */
    String getShopGui();

    /**
     * Id of the GUI the team upgrade shop opens.
     *
     * @since 2.7-beta
     */
    String getUpgradeGui();

    /**
     * The Y level below which players die instantly, or {@code null} while it isn't set.
     *
     * @since 2.7-beta
     */
    default Integer getQuickVoidY() {
        return null;
    }

    /**
     * Online players who are setting this arena up in setup mode.
     *
     * @since 2.7-beta
     */
    default List<Player> getSetupPlayers() {
        return List.of();
    }

    /** Diamond and emerald generators, in the order {@link ArenaEditor#removeGenerator} uses. */
    List<GeneratorView> getGenerators();

    /** Why the arena was loaded disabled, e.g. broken or missing settings. Empty when it loaded fine. */
    default List<String> getLoadProblems() {
        return List.of();
    }

    /**
     * The most players the arena takes: teams times {@link #getMaxInTeam()}.
     *
     * @since 2.8-beta
     */
    default int getMaxPlayers() {
        return getTeams().size() * getMaxInTeam();
    }

    /**
     * The team of a player of this arena; empty for spectators who don't play, for players not in this arena and
     * before the teams are assigned at the game start.
     *
     * @since 2.8-beta
     */
    default Optional<TeamView> getTeam(Player player) {
        if (player == null) return Optional.empty();
        return getGamePlayer(player.getUniqueId()).flatMap(GamePlayerView::getTeam);
    }

    /**
     * Whether the player is a spectator of this arena: someone who joined to watch, a player who is out of the
     * game, or a player waiting for their respawn.
     *
     * @since 2.8-beta
     */
    default boolean isSpectator(Player player) {
        return player != null && getSpectators().contains(player);
    }

    /**
     * The ids of the custom generators the arena's generator file defines ({@code CustomGenerators}), for
     * {@link ArenaEditor#addCustomGenerator}.
     *
     * @since 2.8-beta
     */
    default List<String> getCustomGeneratorIds() {
        return List.of();
    }

    /**
     * Id of the generator config ({@code generators/*.yml}) the arena uses, or {@code null} if none is set.
     *
     * @since 2.8-beta
     */
    default String getGeneratorConfig() {
        return null;
    }

    /**
     * Id of the phase list ({@code GamePhases} in {@code modes.yml}) the arena uses, or {@code null} if none is set.
     *
     * @since 2.8-beta
     */
    default String getGamePhases() {
        return null;
    }

    /**
     * Id of the tab list layout ({@code tablists.yml}) set on this arena, or {@code null} for the server default
     * ({@link com.l299l.newbedwars.api.tablist.TablistService#getDefaultLayout()}).
     *
     * @since 2.8-beta
     */
    default String getTablist() {
        return null;
    }

    /** Data {@code owner} keeps on this arena, saved with the arena file. */
    ExtraData getExtra(Plugin owner);
}
