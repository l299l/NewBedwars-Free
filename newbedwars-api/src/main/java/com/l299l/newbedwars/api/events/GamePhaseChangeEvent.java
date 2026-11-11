package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.HandlerList;

/** Fired when a game moves to its next phase (by the phase timer or an admin). Available from 2.7-beta. */
public class GamePhaseChangeEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String previous;
    private final String next;
    private final boolean forced;

    public GamePhaseChangeEvent(ArenaView arena, String previous, String next, boolean forced) {
        super(arena);
        this.previous = previous;
        this.next = next;
        this.forced = forced;
    }

    /** Name of the phase that ended, or {@code null} at the first phase. */
    public String getPrevious() {
        return previous;
    }

    /** Name of the phase that starts now. */
    public String getNext() {
        return next;
    }

    /** {@code true} when an admin skipped to it ({@code /bw game nextPhase} or the API). */
    public boolean isForced() {
        return forced;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
