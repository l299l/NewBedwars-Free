package com.l299l.newbedwars.api;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** Read-only view of an arena team. */
public interface TeamView {
    String getName();

    ChatColor getColor();

    /** Players currently in the team (a team is alive while it has players). */
    List<Player> getPlayers();

    boolean isAlive();

    boolean isBedDestroyed();

    /** May be {@code null} while the spawn is not configured. */
    Location getSpawn();

    /** Bought level of an upgrade or trap (0 when not bought or unknown). */
    int getUpgradeLevel(String upgradeName);

    /** Settings {@code owner} keeps on this team; saved with the arena file. */
    ExtraData getExtra(Plugin owner);
}
