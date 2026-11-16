package com.l299l.newbedwars.api.view;

import org.bukkit.entity.Player;

import java.util.Optional;

/** A player taking part in an arena. */
public interface GamePlayerView {
    Player getPlayer();

    /** Empty while the arena is waiting, since teams are assigned when the game starts. */
    Optional<TeamView> getTeam();

    int getKills();

    int getFinalKills();

    int getDeaths();

    int getBedsBroken();
}
