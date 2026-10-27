package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** Fired before a player joins (or rejoins) an arena. Cancelling it keeps the player out. */
public class PlayerJoinArenaEvent extends ArenaEvent implements Cancellable {
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

    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
