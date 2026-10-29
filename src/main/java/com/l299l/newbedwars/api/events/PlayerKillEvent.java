package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/** Fired when a player dies in a running game. */
public class PlayerKillEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player victim;
    private final Player killer;
    private final boolean finalKill;
    private final DamageCause cause;

    public PlayerKillEvent(ArenaView arena, Player victim, Player killer, boolean finalKill, DamageCause cause) {
        super(arena);
        this.victim = victim;
        this.killer = killer;
        this.finalKill = finalKill;
        this.cause = cause;
    }

    public Player getVictim() {
        return victim;
    }

    /** The player credited with the kill, or {@code null} (void, fall, environment). */
    public Player getKiller() {
        return killer;
    }

    /** True when the victim's bed was already destroyed, so they will not respawn. */
    public boolean isFinalKill() {
        return finalKill;
    }

    public DamageCause getCause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
