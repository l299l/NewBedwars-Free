package com.l299l.newbedwars.arena.shops;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.entity.Player;

public interface UpgradeHandler {

    default void onPurchase(TeamUpgrades upgrades, Team team, int newLevel) {
        for (Player player : team.getPlayers()) {
            apply(player, team, newLevel);
        }
    }

    default void apply(Player player, Team team, int level) {}

    default boolean reapplyOnRespawn() {
        return false;
    }

    default boolean isTrap() {
        return false;
    }

    default void trigger(Player enemy, Team team, IArena arena) {}
}
