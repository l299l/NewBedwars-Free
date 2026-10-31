package com.l299l.newbedwars.arena.player;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/** Puts a player into a clean state when they join, leave, spectate or respawn. */
public final class PlayerStateResetter {
    private PlayerStateResetter() {}

    /** Sets the game mode, full health and food, and clears inventory, armor, exp, fire and flight. */
    public static void reset(Player player, GameMode gameMode) {
        player.setGameMode(gameMode);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setExp(0);
        player.setLevel(0);
        player.setFireTicks(0);
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    public static void clearEffects(Player player) {
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
    }
}
