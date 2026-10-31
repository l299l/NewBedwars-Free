package com.l299l.newbedwars.api.edit;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

/** Changes one team of an arena; see {@link ArenaEditor} for the rules. */
public interface TeamEditor {
    String getName();

    void setColor(ChatColor color);

    void setSpawn(Location location);

    /** @param location the bed block; a bed must be placed there */
    void setBed(Location location);

    void clearBed();

    /** @param npcType living entity type of the shop NPC */
    void setShop(Location location, EntityType npcType);

    void clearShop();

    /** @param npcType living entity type of the upgrades NPC */
    void setUpgrades(Location location, EntityType npcType);

    void clearUpgrades();

    /** Sets the team's iron/gold generator. */
    void setGenerator(Location location);

    void clearGenerator();
}
