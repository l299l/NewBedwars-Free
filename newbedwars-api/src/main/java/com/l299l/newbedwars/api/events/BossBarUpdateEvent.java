package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;

/**
 * Fired when a boss bar of {@code bossBars.yml} switches to a new text: a step of its {@code Texts} cycle, or when
 * an arena starts showing it (the phase changes). It is not fired for bars shown by addons, nor for the
 * every-second refresh of the same text. Change the display with the setters. A text you change stays as it is
 * (it is no longer refreshed) until the next step. Cancelling it keeps the previous display for this step; the
 * first display of a bar (it has no previous one) is shown as configured.
 *
 * @since 2.8-beta
 */
public class BossBarUpdateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ArenaView arena;
    private final String bossBar;
    private String text;
    private BarColor color;
    private BarStyle style;
    private boolean cancelled;

    public BossBarUpdateEvent(ArenaView arena, String bossBar, String text, BarColor color, BarStyle style) {
        this.arena = arena;
        this.bossBar = bossBar;
        this.text = text == null ? "" : text;
        this.color = color;
        this.style = style;
    }

    /** The arena the bar belongs to, empty for the lobby bar ({@code MainLobby}). */
    public Optional<ArenaView> getArena() {
        return arena == null ? Optional.empty() : Optional.of(arena);
    }

    /** The id of the boss bar in {@code bossBars.yml}. */
    public String getBossBar() {
        return bossBar;
    }

    /** The text with placeholders resolved and colors translated. */
    public String getText() {
        return text;
    }

    /** {@code null} counts as empty. */
    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    public BarColor getColor() {
        return color;
    }

    /** {@code null} is ignored. */
    public void setColor(BarColor color) {
        if (color != null) this.color = color;
    }

    public BarStyle getStyle() {
        return style;
    }

    /** {@code null} is ignored. */
    public void setStyle(BarStyle style) {
        if (style != null) this.style = style;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
