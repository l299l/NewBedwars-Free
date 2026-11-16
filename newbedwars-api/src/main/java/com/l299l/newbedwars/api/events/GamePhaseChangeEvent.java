package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.HandlerList;

/**
 * Fired when a game moves to its next phase, after the phase timer or because an admin skipped it.
 *
 * @since 2.7-beta
 */
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

    /** The phase that ended, or {@code null} when the first phase starts. */
    public String getPrevious() {
        return previous;
    }

    /** The phase that starts now. */
    public String getNext() {
        return next;
    }

    /** Whether an admin skipped to it with {@code /bw game nextPhase} or the API. */
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
