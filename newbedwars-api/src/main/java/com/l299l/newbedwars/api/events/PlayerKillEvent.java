package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Optional;

/**
 * Fired when a player dies in a running game, before they are turned into a spectator to wait for their
 * respawn or, after a final kill, for good.
 */
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

    /**
     * The player credited with the kill, or {@code null} when nobody gets it (fall damage, lava, a dragon...).
     * A player who hit the victim shortly before a fall or void death still gets the kill.
     */
    public Player getKiller() {
        return killer;
    }

    /** Whether the victim's bed was already gone, so they won't respawn. */
    public boolean isFinalKill() {
        return finalKill;
    }

    /**
     * What killed the victim. Every death in the void is {@code VOID}, including the arena's quick void and
     * being knocked off by a player. {@code CUSTOM} means the server reported no cause.
     */
    public DamageCause getCause() {
        return cause;
    }

    /** The team whose dragon killed the victim. Empty when it wasn't a dragon. */
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
