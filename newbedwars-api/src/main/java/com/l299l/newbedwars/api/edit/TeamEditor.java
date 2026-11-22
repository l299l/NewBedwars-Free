package com.l299l.newbedwars.api.edit;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

/** Changes one team of an arena. The rules of {@link ArenaEditor} apply. */
public interface TeamEditor {
    String getName();

    /** @param color a color, not a format code like {@code BOLD} */
    void setColor(ChatColor color);

    /**
     * Sets the name players see for this team, e.g. {@code "Lime"} after {@link #setColor} gave the team
     * {@code Red} a lime color. {@link #getName()} stays the same, so the team keeps its setup, upgrades and
     * commands. The display name is saved with the arena like the color; {@code null} or an empty text shows
     * {@link #getName()} again. Colour codes are not allowed: the team color is used.
     *
     * @param displayName 1 to 32 characters without {@code §} colour codes or line breaks, or null
     * @throws IllegalArgumentException for an invalid display name
     * @since 2.8-beta
     */
    void setDisplayName(String displayName);

    void setSpawn(Location location);

    /** @param location either half of a bed that is already placed there */
    void setBed(Location location);

    void clearBed();

    /** @param npcType the shop NPC's entity type, must be a living entity */
    void setShop(Location location, EntityType npcType);

    void clearShop();

    /** @param npcType the upgrades NPC's entity type, must be a living entity */
    void setUpgrades(Location location, EntityType npcType);

    void clearUpgrades();

    /** Sets the team's iron and gold generator. */
    void setGenerator(Location location);

    void clearGenerator();
}
