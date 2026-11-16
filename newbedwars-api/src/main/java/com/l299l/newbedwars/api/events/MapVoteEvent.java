package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player votes for the next map during the after-game time of {@link #getArena()}. Cancelling it
 * ignores the vote.
 */
public class MapVoteEvent extends CancellableArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ArenaView voted;
    private final com.l299l.newbedwars.api.view.NetworkArenaView votedNetwork;

    public MapVoteEvent(ArenaView arena, Player player, ArenaView voted) {
        this(arena, player, voted, null);
    }

    /**
     * Creates the event.
     *
     * @param votedNetwork the voted arena when it is on another game server, {@code voted} is then {@code null}
     */
    public MapVoteEvent(ArenaView arena, Player player, ArenaView voted, com.l299l.newbedwars.api.view.NetworkArenaView votedNetwork) {
        super(arena);
        this.player = player;
        this.voted = voted;
        this.votedNetwork = votedNetwork;
    }

    public Player getPlayer() {
        return player;
    }

    /** The voted arena when it is on another game server. {@link #getVotedArena()} is {@code null} then. */
    public java.util.Optional<com.l299l.newbedwars.api.view.NetworkArenaView> getVotedNetworkArena() {
        return java.util.Optional.ofNullable(votedNetwork);
    }

    /** The voted arena on this server, or {@code null} when it is on another server. */
    public ArenaView getVotedArena() {
        return voted;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
