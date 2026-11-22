package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.PartyView;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired after a party on this server changed. In network mode the proxy owns the parties; changes that arrive
 * from the proxy don't fire this event.
 *
 * @since 2.8-beta
 */
public class PartyChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final PartyView party;
    private final UUID player;
    private final Change change;

    /** What happened to the party. */
    public enum Change {
        /** The party was created; {@link #getPlayer()} is its leader. */
        CREATED,
        /** {@link #getPlayer()} accepted an invite. */
        JOINED,
        /** {@link #getPlayer()} left the party. */
        LEFT,
        /** {@link #getPlayer()} was kicked by the leader. */
        KICKED,
        /** {@link #getPlayer()} is the new leader. */
        LEADER_CHANGED,
        /** The party was disbanded (by its leader, or because its last member left); {@link #getPlayer()} is null. */
        DISBANDED
    }

    public PartyChangeEvent(PartyView party, UUID player, Change change) {
        this.party = party;
        this.player = player;
        this.change = change;
    }

    /** The party after the change; empty after {@link Change#DISBANDED}. */
    public PartyView getParty() {
        return party;
    }

    /** The player the change is about; null for {@link Change#DISBANDED}. */
    public UUID getPlayer() {
        return player;
    }

    public Change getChange() {
        return change;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
