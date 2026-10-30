package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player of the ended arena {@link #getArena()} votes for the next map during the after-game time.
 * Cancelling it ignores the vote.
 */
public class MapVoteEvent extends CancellableArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ArenaView voted;

    public MapVoteEvent(ArenaView arena, Player player, ArenaView voted) {
        super(arena);
        this.player = player;
        this.voted = voted;
    }

    public Player getPlayer() {
        return player;
    }

    /** The arena (map) the player voted for. */
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
