package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import java.util.List;
import org.bukkit.event.HandlerList;

/** Fired when the bed destruction phase destroys the remaining beds (beds broken by players fire {@link BedBreakEvent}). Available from 2.7-beta. */
public class BedsDestroyedEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final List<TeamView> teams;

    public BedsDestroyedEvent(ArenaView arena, List<TeamView> teams) {
        super(arena);
        this.teams = teams;
    }

    /** Teams whose bed was still standing. */
    public List<TeamView> getTeams() {
        return teams;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
