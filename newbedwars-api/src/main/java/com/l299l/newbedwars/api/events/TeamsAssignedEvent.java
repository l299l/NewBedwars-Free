package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.HandlerList;

/**
 * Fired at the game start after every player was put into a team, before the players are teleported to
 * their team spawns and before {@link GameStartEvent}. {@code getArena().getTeams()} shows the teams with their players.
 *
 * @since 2.8-beta
 */
public class TeamsAssignedEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public TeamsAssignedEvent(ArenaView arena) {
        super(arena);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
