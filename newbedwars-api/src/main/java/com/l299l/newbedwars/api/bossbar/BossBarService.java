package com.l299l.newbedwars.api.bossbar;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * The boss bars of {@code bossBars.yml} and boss bars shown by addons. Get it with
 * {@link com.l299l.newbedwars.api.NewBedwarsAPI#getBossBars()}.
 * <p>
 * Call the methods on the server thread.
 *
 * @since 2.8-beta
 */
public interface BossBarService {
    /** Ids of the boss bars in {@code bossBars.yml}, sorted. */
    List<String> getBossBars();

    /** Whether the boss bar exists and has {@code Enabled: true}. */
    boolean isEnabled(String id);

    /**
     * Shows a boss bar to one player. The text supports {@code &} color codes, the {@code /name/} placeholders
     * of {@code bossBars.yml}, placeholders registered with
     * {@link com.l299l.newbedwars.api.NewBedwarsAPI#registerPlaceholder} and PlaceholderAPI when it is installed;
     * a text with placeholders is resolved again every second.
     * <p>
     * The bar is removed when the player quits, leaves or changes the arena (when it was shown while the player
     * was in one), when {@code owner} is disabled or
     * {@link com.l299l.newbedwars.api.NewBedwarsAPI#unregisterAll} is called, and when NewBedwars is disabled.
     *
     * @param progress the fill, clamped to {@code 0..1}
     * @throws IllegalArgumentException for a {@code null} owner, player, color or style
     */
    AddonBossBar show(Plugin owner, Player player, String text, BarColor color, BarStyle style, double progress);
}
