package com.l299l.newbedwars.api.edit;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.view.GeneratorKind;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Optional;

/**
 * Changes an arena's setup: teams, spawns, beds, shops, upgrade NPCs and generators.
 * <p>
 * Only works while the arena is {@link ArenaState#WAITING}; otherwise every method throws
 * {@link IllegalStateException}. Invalid input throws {@link IllegalArgumentException}, for example a
 * location outside the arena's world. Changes are written to the arena file shortly after, or right away
 * with {@link #save()}.
 */
public interface ArenaEditor {
    /** @param minPlayers at least 1 */
    void setMinPlayers(int minPlayers);

    /** @param maxInTeam at least 1 */
    void setMaxInTeam(int maxInTeam);

    void setWaitingSpawn(Location location);

    /**
     * Adds a team.
     *
     * @param color a color, not a format code like {@code BOLD}
     * @throws IllegalArgumentException when the name is blank or a team with that name exists
     */
    TeamEditor createTeam(String name, ChatColor color);

    Optional<TeamEditor> getTeam(String name);

    /** Returns {@code false} when there is no such team. */
    boolean removeTeam(String name);

    /**
     * Adds a diamond or emerald generator. Team generators are set with {@link TeamEditor#setGenerator}.
     *
     * @param kind {@link GeneratorKind#DIAMOND} or {@link GeneratorKind#EMERALD}
     */
    void addGenerator(GeneratorKind kind, Location location);

    /**
     * Removes a diamond or emerald generator.
     *
     * @param index position in {@link ArenaView#getGenerators()}
     * @return {@code false} for an invalid index
     */
    boolean removeGenerator(int index);

    /**
     * Sets the GUI the item shop opens.
     *
     * @throws IllegalArgumentException when there is no GUI with that id
     * @since 2.7-beta
     */
    void setShopGui(String guiId);

    /**
     * Sets the GUI the team upgrade shop opens.
     *
     * @throws IllegalArgumentException when there is no GUI with that id
     * @since 2.7-beta
     */
    void setUpgradeGui(String guiId);

    /** Writes the arena file now. */
    void save();
}
