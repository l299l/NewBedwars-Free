package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired before an enemy bed is destroyed. Cancelling it leaves the bed intact. */
public class BedBreakEvent extends CancellableArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final TeamView team;
    private final Player breaker;

    public BedBreakEvent(ArenaView arena, TeamView team, Player breaker) {
        super(arena);
        this.team = team;
        this.breaker = breaker;
    }

    /** The team whose bed is broken. */
    public TeamView getTeam() {
        return team;
    }

    public Player getBreaker() {
        return breaker;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
