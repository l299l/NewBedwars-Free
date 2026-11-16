package com.l299l.newbedwars.api.view;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** A team of an arena. */
public interface TeamView {
    String getName();

    ChatColor getColor();

    /**
     * Players in the team, including ones waiting to respawn. A player leaves the team after a final kill or
     * when they leave the game.
     */
    List<Player> getPlayers();

    /** Whether the team still has players. A team that lost its bed stays alive until its last player is out. */
    boolean isAlive();

    boolean isBedDestroyed();

    /** {@code null} while the spawn isn't set. */
    Location getSpawn();

    /**
     * Where the bed was set (usually its head block), or {@code null} while the bed isn't set.
     *
     * @since 2.7-beta
     */
    default Location getBed() {
        return null;
    }

    /** The bought level of an upgrade or trap, 0 when it isn't bought or doesn't exist. */
    int getUpgradeLevel(String upgradeName);

    /** Data {@code owner} keeps on this team, saved with the arena file. */
    ExtraData getExtra(Plugin owner);
}
