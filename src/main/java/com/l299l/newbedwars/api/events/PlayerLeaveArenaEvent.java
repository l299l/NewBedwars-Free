package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired when a player (or spectator) leaves an arena, before they are sent to the lobby. */
public class PlayerLeaveArenaEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final LeaveReason reason;

    public PlayerLeaveArenaEvent(ArenaView arena, Player player, LeaveReason reason) {
        super(arena);
        this.player = player;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public LeaveReason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
