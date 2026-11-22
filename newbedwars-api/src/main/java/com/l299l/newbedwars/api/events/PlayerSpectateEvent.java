package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired before a player who is not playing starts spectating a running game ({@code /bw spectate}, the spectate menu,
 * {@link com.l299l.newbedwars.api.NewBedwarsAPI#spectate}). Cancelling it keeps the player out.
 * Players who die and wait for their respawn, or who are out of the game, don't fire it.
 *
 * @since 2.8-beta
 */
public class PlayerSpectateEvent extends CancellableArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public PlayerSpectateEvent(ArenaView arena, Player player) {
        super(arena);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
