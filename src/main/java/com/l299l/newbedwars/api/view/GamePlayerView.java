package com.l299l.newbedwars.api.view;

import org.bukkit.entity.Player;

import java.util.Optional;

/** Read-only view of a player taking part in an arena. */
public interface GamePlayerView {
    Player getPlayer();

    /** The team of the player, empty while the arena is waiting (teams are assigned at game start). */
    Optional<TeamView> getTeam();

    int getKills();

    int getFinalKills();

    int getDeaths();

    int getBedsBroken();
}
