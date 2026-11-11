package com.l299l.newbedwars.api.view;

import com.l299l.newbedwars.api.edit.ArenaEditor;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only view of an arena. Obtained from {@link NewBedwarsAPI}. */
public interface ArenaView {
    String getName();

    ArenaState getState();

    boolean isEnabled();

    /** The arena world; it is replaced after every map reset, so do not keep it. */
    World getWorld();

    int getMinPlayers();

    int getMaxInTeam();

    /** Players of the arena (not spectators). */
    List<Player> getPlayers();

    List<Player> getSpectators();

    List<TeamView> getTeams();

    Optional<TeamView> getTeam(String name);

    Optional<GamePlayerView> getGamePlayer(UUID player);

    /** Name of the current game phase, or {@code null} when none is running. */
    String getCurrentPhase();

    /** Seconds since the game started. */
    int getGameTime();

    /** Name of the phase that comes after the current one, or {@code null} when there is none. Available from 2.7-beta. */
    String getNextPhase();

    /** Seconds until the current phase ends while a game runs, or -1 when no phase timer runs. Available from 2.7-beta. */
    int getPhaseTimeLeft();

    /** Id of the shop GUI the arena's item shop opens. Available from 2.7-beta. */
    String getShopGui();

    /** Id of the GUI the arena's team upgrade shop opens. Available from 2.7-beta. */
    String getUpgradeGui();

    /** Map generators (diamond, emerald) in the order used by {@link ArenaEditor#removeGenerator}. */
    List<GeneratorView> getGenerators();

    /** Why the arena was loaded disabled (broken or missing settings); empty when it loaded fine. */
    default List<String> getLoadProblems() {
        return List.of();
    }

    /** Settings {@code owner} keeps on this arena; saved with the arena file. */
    ExtraData getExtra(Plugin owner);
}
