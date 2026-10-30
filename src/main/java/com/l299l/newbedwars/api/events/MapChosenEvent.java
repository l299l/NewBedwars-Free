package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaGroupView;
import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.HandlerList;

/**
 * Fired when the after-game time of {@link #getArena()} is over and {@link #getChosenArena()} won the next-map
 * vote. The arena then stops and everyone still in it joins the chosen arena.
 */
public class MapChosenEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ArenaGroupView group;
    private final ArenaView chosen;

    public MapChosenEvent(ArenaView arena, ArenaGroupView group, ArenaView chosen) {
        super(arena);
        this.group = group;
        this.chosen = chosen;
    }

    public ArenaGroupView getGroup() {
        return group;
    }

    public ArenaView getChosenArena() {
        return chosen;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
