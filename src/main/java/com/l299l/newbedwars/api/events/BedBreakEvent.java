package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.ArenaView;
import com.l299l.newbedwars.api.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** Fired before an enemy bed is destroyed. Cancelling it leaves the bed intact. */
public class BedBreakEvent extends ArenaEvent implements Cancellable {
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

    private boolean cancelled;

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
