package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fired just before NewBedwars sends a changed sidebar scoreboard (title or lines) to a player. It is not fired
 * while nothing changed. Change the content with the setters; whatever is set afterwards is shown. A title you
 * change replaces the animated {@code TitleEffect} of the scoreboard. Cancelling it sends nothing for this change;
 * the next change fires it again, or use
 * {@link com.l299l.newbedwars.api.scoreboard.ScoreboardService#refresh(Player)}.
 *
 * @since 2.8-beta
 */
public class ScoreboardUpdateEvent extends Event implements Cancellable {
    /** The most lines a sidebar can show. */
    public static final int MAX_LINES = 15;

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ArenaView arena;
    private final String scoreboard;
    private String title;
    private List<String> lines;
    private boolean cancelled;

    public ScoreboardUpdateEvent(Player player, ArenaView arena, String scoreboard, String title, List<String> lines) {
        this.player = player;
        this.arena = arena;
        this.scoreboard = scoreboard;
        this.title = title == null ? "" : title;
        this.lines = new ArrayList<>(lines);
    }

    /** The player whose scoreboard changes. */
    public Player getPlayer() {
        return player;
    }

    /** The arena the player plays or spectates in. */
    public Optional<ArenaView> getArena() {
        return Optional.ofNullable(arena);
    }

    /** The id of the scoreboard in {@code scoreboards.yml}. */
    public String getScoreboard() {
        return scoreboard;
    }

    /** The title with colors translated. */
    public String getTitle() {
        return title;
    }

    /** {@code null} counts as empty. */
    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    /** A copy of the lines, top to bottom, colors translated. Changing the list does nothing; use the setter. */
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }

    /**
     * Replaces the lines (copied). Lines that are equal are made unique automatically.
     *
     * @throws IllegalArgumentException for {@code null}, a {@code null} line or more than {@link #MAX_LINES} lines
     */
    public void setLines(List<String> lines) {
        if (lines == null) throw new IllegalArgumentException("lines is null");
        if (lines.size() > MAX_LINES) throw new IllegalArgumentException("a scoreboard has at most " + MAX_LINES + " lines");
        for (String line : lines) {
            if (line == null) throw new IllegalArgumentException("a line is null");
        }
        this.lines = new ArrayList<>(lines);
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
