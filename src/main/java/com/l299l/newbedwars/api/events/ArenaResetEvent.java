package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.ArenaView;
import org.bukkit.event.HandlerList;

/** Fired twice per map reset: before it starts and after the arena world is ready again. */
public class ArenaResetEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Stage stage;

    public ArenaResetEvent(ArenaView arena, Stage stage) {
        super(arena);
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }

    public enum Stage {
        /** The reset is about to begin; the arena world is still the old one. */
        PRE,
        /** The map is restored and the arena world is ready again. */
        POST
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
