package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Optional;

/** Fired when a player dies in a running game. */
public class PlayerKillEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player victim;
    private final Player killer;
    private final boolean finalKill;
    private final DamageCause cause;
    private final TeamView dragonTeam;

    public PlayerKillEvent(ArenaView arena, Player victim, Player killer, boolean finalKill, DamageCause cause) {
        this(arena, victim, killer, finalKill, cause, null);
    }

    public PlayerKillEvent(ArenaView arena, Player victim, Player killer, boolean finalKill, DamageCause cause, TeamView dragonTeam) {
        super(arena);
        this.victim = victim;
        this.killer = killer;
        this.finalKill = finalKill;
        this.cause = cause;
        this.dragonTeam = dragonTeam;
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

    /**
     * What killed the victim. {@code VOID} for every fall into the void, also below the arena's quick-void height
     * and when another player knocked the victim off; {@code CUSTOM} only when the server reported no cause.
     */
    public DamageCause getCause() {
        return cause;
    }

    /** The team whose Dragon Buff dragon killed the victim, or empty when no dragon did. */
    public Optional<TeamView> getDragonTeam() {
        return Optional.ofNullable(dragonTeam);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
