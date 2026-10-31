package com.l299l.newbedwars.arena.shops;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.entity.Player;

/** What a team upgrade or trap does. Built-in ones live in {@link BuiltInUpgrades}; addons register their own. */
public interface UpgradeHandler {
    /** Called when the team buys the upgrade (not for traps); the default applies the effect to every current team member. */
    default void onPurchase(TeamUpgrades upgrades, Team team, int newLevel) {
        for (Player player : team.getPlayers()) {
            apply(player, team, newLevel);
        }
    }

    /** Applies the effect to one player (on purchase, and on respawn when {@link #reapplyOnRespawn()}). */
    default void apply(Player player, Team team, int level) {}

    default boolean reapplyOnRespawn() {
        return false;
    }

    /** A trap has no effect when bought; it is used up when an enemy enters the base. */
    default boolean isTrap() {
        return false;
    }

    default void trigger(Player enemy, Team team, IArena arena) {}
}
