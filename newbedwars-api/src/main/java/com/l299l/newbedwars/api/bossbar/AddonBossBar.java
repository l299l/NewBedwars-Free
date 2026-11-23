package com.l299l.newbedwars.api.bossbar;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;

/**
 * A boss bar an addon shows to one player, created with
 * {@link BossBarService#show(org.bukkit.plugin.Plugin, Player, String, BarColor, BarStyle, double)}.
 *
 * @since 2.8-beta
 */
public interface AddonBossBar {
    /** The player who sees the bar. */
    Player getPlayer();

    /** Changes the text; {@code &} colors and placeholders work as in {@link BossBarService#show}. */
    void setText(String text);

    void setColor(BarColor color);

    void setStyle(BarStyle style);

    /** Sets the fill; values outside {@code 0..1} are clamped. */
    void setProgress(double progress);

    /** {@code false} once the bar was removed, by {@link #remove()} or automatically. */
    boolean isVisible();

    /** Removes the bar from the player. It does nothing when the bar is already removed. */
    void remove();
}
