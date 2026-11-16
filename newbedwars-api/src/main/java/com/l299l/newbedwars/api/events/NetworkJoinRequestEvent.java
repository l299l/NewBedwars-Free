package com.l299l.newbedwars.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on a lobby server right before a join, rejoin or spectate request goes to the proxy, from a command,
 * a join NPC, the arena selector or the API. Cancelling it stops the request. NewBedwars sends the player no
 * message then, so tell them why.
 */
public class NetworkJoinRequestEvent extends Event implements Cancellable {
    public enum Kind {
        /** Any arena of the network. */
        ANY,
        /** An arena group, name pattern or team size. */
        GROUP,
        /** One named arena. */
        ARENA,
        /** Back into the game the player left. */
        REJOIN,
        /** Watch a running game; the target is the arena name. */
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

    /** The player who asked. For a party this is its admin, and the whole party is moved. */
    public Player getPlayer() {
        return player;
    }

    public Kind getKind() {
        return kind;
    }

    /** The group or arena name, {@code null} for {@link Kind#ANY} and {@link Kind#REJOIN}. */
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
