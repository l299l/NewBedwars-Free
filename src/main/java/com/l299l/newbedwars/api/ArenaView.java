package com.l299l.newbedwars.api;

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

    /** Map generators (diamond, emerald) in the order used by {@link ArenaEditor#removeGenerator}. */
    List<GeneratorView> getGenerators();

    /** Settings {@code owner} keeps on this arena; saved with the arena file. */
    ExtraData getExtra(Plugin owner);
}
