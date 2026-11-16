package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaGroupView;
import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.HandlerList;

/**
 * Fired when the after-game time of {@link #getArena()} is over and a map won the next-map vote. The arena
 * then stops and everyone still in it joins the winner.
 */
public class MapChosenEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ArenaGroupView group;
    private final ArenaView chosen;
    private final com.l299l.newbedwars.api.view.NetworkArenaView chosenNetwork;

    public MapChosenEvent(ArenaView arena, ArenaGroupView group, ArenaView chosen) {
        this(arena, group, chosen, null);
    }

    /**
     * Creates the event.
     *
     * @param chosenNetwork the winner when it is on another game server, {@code chosen} is then {@code null}
     */
    public MapChosenEvent(ArenaView arena, ArenaGroupView group, ArenaView chosen,
                          com.l299l.newbedwars.api.view.NetworkArenaView chosenNetwork) {
        super(arena);
        this.group = group;
        this.chosen = chosen;
        this.chosenNetwork = chosenNetwork;
    }

    /** The winner when it is on another game server. {@link #getChosenArena()} is {@code null} then. */
    public java.util.Optional<com.l299l.newbedwars.api.view.NetworkArenaView> getChosenNetworkArena() {
        return java.util.Optional.ofNullable(chosenNetwork);
    }

    public ArenaGroupView getGroup() {
        return group;
    }

    /** The winner on this server, or {@code null} when it is on another server. */
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
