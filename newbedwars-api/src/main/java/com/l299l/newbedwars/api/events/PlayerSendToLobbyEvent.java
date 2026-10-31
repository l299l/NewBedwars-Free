package com.l299l.newbedwars.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired whenever NewBedwars sends a player to the lobby. Cancel it and handle the player yourself (for example
 * send them to a lobby server on a network), or change the target.
 */
public class PlayerSendToLobbyEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Reason reason;
    private Location target;

    public PlayerSendToLobbyEvent(Player player, Reason reason, Location target) {
        this.player = player;
        this.reason = reason;
        this.target = target;
    }

    public Player getPlayer() {
        return player;
    }

    public Reason getReason() {
        return reason;
    }

    /** Where the player will be teleported. */
    public Location getTarget() {
        return target;
    }

    public void setTarget(Location target) {
        this.target = target;
    }

    public enum Reason {
        LEFT_ARENA,
        LOBBY_COMMAND,
        JOINED_IN_ARENA_WORLD,
        MAP_RESET,
        ARENA_UNLOADED,
        ARENA_DELETED,
        SETUP_ENDED,
        SERVER_SHUTDOWN,
        RELOAD
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
