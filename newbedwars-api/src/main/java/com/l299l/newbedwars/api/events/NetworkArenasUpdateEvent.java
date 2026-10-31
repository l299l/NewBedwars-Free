package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.NetworkArenaView;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collection;

/** Fired on a lobby server when the list of network arenas ({@code NewBedwarsAPI.getNetworkArenas()}) changed. */
public class NetworkArenasUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Collection<NetworkArenaView> arenas;

    public NetworkArenasUpdateEvent(Collection<NetworkArenaView> arenas) {
        this.arenas = arenas;
    }

    /** The new list (unmodifiable). */
    public Collection<NetworkArenaView> getArenas() {
        return arenas;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
