package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.ArenaView;
import org.bukkit.event.Event;

/** Base of all events that happen inside one arena. */
public abstract class ArenaEvent extends Event {
    private final ArenaView arena;

    protected ArenaEvent(ArenaView arena) {
        this.arena = arena;
    }

    public ArenaView getArena() {
        return arena;
    }
}
