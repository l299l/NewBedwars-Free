package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.HandlerList;

/**
 * Fired after an arena changed its {@link ArenaState}.
 * <p>
 * {@code WAITING -> STARTING} is the start of the countdown, {@code STARTING -> WAITING} a cancelled countdown,
 * {@code STARTING -> PLAYING} (or {@code WAITING -> PLAYING} on a forced start) the game start,
 * {@code PLAYING -> ENDING} the end of the game and {@code ENDING -> RESTARTING -> WAITING} the map reset.
 * Disabling an arena moves it to {@code RESTARTING}, enabling it to {@code WAITING}.
 *
 * @since 2.8-beta
 */
public class ArenaStateChangeEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ArenaState previous;
    private final ArenaState next;

    public ArenaStateChangeEvent(ArenaView arena, ArenaState previous, ArenaState next) {
        super(arena);
        this.previous = previous;
        this.next = next;
    }

    /** The state before the change. */
    public ArenaState getPrevious() {
        return previous;
    }

    /** The new state, the same as {@code getArena().getState()}. */
    public ArenaState getNext() {
        return next;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
