package com.l299l.newbedwars.bossbar;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public class BossBarSave {
    private final boolean animated;
    private final boolean enabled;
    private final String[] texts;
    private final BarColor[] color;
    private final int time;
    private final BarStyle[] style;
    private final TextEffect[] effects;
    private final BarAction action;

    public BossBarSave(boolean enabled, String[] texts, BarColor[] color, int time, BarStyle[] style, TextEffect[] effects, BarAction action) {
        this.enabled = enabled;
        this.texts = texts;
        for (int i = 0; i < texts.length; i++) {
            texts[i] = ChatColor.translateAlternateColorCodes('&', texts[i]);
        }
        this.color = color;
        this.time = time;
        this.style = style;
        this.effects = effects;
        this.action = action;
        animated = texts.length != 1;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAnimated() {
        return animated;
    }

    public String[] getTexts() {
        return texts;
    }

    public BarColor[] getColor() {
        return color;
    }

    public int getTime() {
        return time;
    }

    public BarStyle[] getStyle() {
        return style;
    }

    public TextEffect[] getEffects() {
        return effects;
    }

    public BarAction getAction() {
        return action;
    }
}
