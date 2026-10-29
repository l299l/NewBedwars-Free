package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.event.HandlerList;

/** Fired when a game ends, before the winners are announced. */
public class GameEndEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final TeamView winner;

    public GameEndEvent(ArenaView arena, TeamView winner) {
        super(arena);
        this.winner = winner;
    }

    /** The winning team, or {@code null} for a draw. */
    public TeamView getWinner() {
        return winner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
