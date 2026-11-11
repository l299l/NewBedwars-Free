package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.event.HandlerList;

/** Fired when a team is out of the game (no bed and no living players left). Available from 2.7-beta. */
public class TeamEliminatedEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final TeamView team;

    public TeamEliminatedEvent(ArenaView arena, TeamView team) {
        super(arena);
        this.team = team;
    }

    /** The team that is out. */
    public TeamView getTeam() {
        return team;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
