package com.l299l.newbedwars.api.edit;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

/** Changes one team of an arena. The rules of {@link ArenaEditor} apply. */
public interface TeamEditor {
    String getName();

    /** @param color a color, not a format code like {@code BOLD} */
    void setColor(ChatColor color);

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
