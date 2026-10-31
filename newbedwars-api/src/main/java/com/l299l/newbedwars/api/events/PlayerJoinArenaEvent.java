package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired before a player joins (or rejoins) an arena. Cancelling it keeps the player out. */
public class PlayerJoinArenaEvent extends CancellableArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final boolean rejoin;

    public PlayerJoinArenaEvent(ArenaView arena, Player player, boolean rejoin) {
        super(arena);
        this.player = player;
        this.rejoin = rejoin;
    }

    public Player getPlayer() {
        return player;
    }

    /** True when the player returns to a running game they left. */
    public boolean isRejoin() {
        return rejoin;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
