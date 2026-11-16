package com.l299l.newbedwars.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired whenever NewBedwars sends a player to the lobby. Change the target, or cancel it and handle the
 * player yourself, e.g. send them to a lobby server of a network.
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
        /** Left an arena or was sent out of one, including after the game. */
        LEFT_ARENA,
        /** Used {@code /bw lobby} or {@code /lobby}. */
        LOBBY_COMMAND,
        /** Joined the server inside an arena world. */
        JOINED_IN_ARENA_WORLD,
        /** Was in an arena world when its map was reset. */
        MAP_RESET,
        /** Was in an arena world when the arena was unloaded. */
        ARENA_UNLOADED,
        /** Was in an arena world when the arena was deleted. */
        ARENA_DELETED,
        /** Left setup mode. */
        SETUP_ENDED,
        /** The server is stopping. */
        SERVER_SHUTDOWN,
        /** {@code /bw reload} sent players in setup mode out. */
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
