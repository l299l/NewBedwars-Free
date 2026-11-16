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

    /** Data {@code owner} keeps on this arena, saved with the arena file. */
    ExtraData getExtra(Plugin owner);
}
