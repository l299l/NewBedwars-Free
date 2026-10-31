package com.l299l.newbedwars.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on a LOBBY server right before a join or rejoin request is sent to the proxy ({@code /bw join},
 * join NPCs, the arena selector, {@code /bw rejoin}, {@code /bw spectate} or the API). Cancel it to stop the request; the player
 * gets no message from NewBedwars then, so tell them why.
 */
public class NetworkJoinRequestEvent extends Event implements Cancellable {
    /** What the request aims at. */
    public enum Kind {
        /** Any arena of the network. */
        ANY,
        /** An arena group (or name pattern / team size). */
        GROUP,
        /** One named arena. */
        ARENA,
        /** Back into the running game the player left. */
        REJOIN,
        /** Watch a running game (target = arena name). */
        SPECTATE
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Kind kind;
    private final String target;
    private boolean cancelled;

    public NetworkJoinRequestEvent(Player player, Kind kind, String target) {
        this.player = player;
        this.kind = kind;
        this.target = target;
    }

    /** The player who asked (for a party: its admin; the whole party is moved). */
    public Player getPlayer() {
        return player;
    }

    public Kind getKind() {
        return kind;
    }

    /** The group or arena name; null for {@link Kind#ANY} and {@link Kind#REJOIN}. */
    public String getTarget() {
        return target;
    }

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
