package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.HandlerList;

/** Fired when a game starts, after the teams were assigned. */
public class GameStartEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public GameStartEvent(ArenaView arena) {
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
