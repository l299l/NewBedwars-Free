package com.l299l.newbedwars.api;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Optional;

/**
 * Changes an arena's setup from code: teams, spawns, beds, shops, upgrade NPCs and generators. Only possible
 * while the arena is {@link ArenaState#WAITING}; every method throws {@link IllegalStateException} otherwise.
 * Changes are kept in memory and written to the arena file shortly after (or call {@link #save()}).
 * Invalid input throws {@link IllegalArgumentException}.
 */
public interface ArenaEditor {
    void setMinPlayers(int minPlayers);

    void setMaxInTeam(int maxInTeam);

    void setWaitingSpawn(Location location);

    TeamEditor createTeam(String name, ChatColor color);

    Optional<TeamEditor> getTeam(String name);

    /** @return false when there is no such team */
    boolean removeTeam(String name);

    /** Adds a map generator ({@link GeneratorKind#DIAMOND} or {@link GeneratorKind#EMERALD}). */
    void addGenerator(GeneratorKind kind, Location location);

    /** @param index position in {@link ArenaView#getGenerators()} @return false for an invalid index */
    boolean removeGenerator(int index);

    /** Writes the arena file now. */
    void save();
}
