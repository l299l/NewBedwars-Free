package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired before one trap of a team is set off by an enemy entering the team's base. A team with several traps
 * fires one event per trap. Cancelling it keeps the trap: it is not used up and has no effect.
 *
 * @since 2.8-beta
 */
public class TrapTriggerEvent extends CancellableArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final TeamView team;
    private final Player enemy;
    private final String trap;

    public TrapTriggerEvent(ArenaView arena, TeamView team, Player enemy, String trap) {
        super(arena);
        this.team = team;
        this.enemy = enemy;
        this.trap = trap;
    }

    /** The team that owns the trap. */
    public TeamView getTeam() {
        return team;
    }

    /** The player who walked into the base. */
    public Player getEnemy() {
        return enemy;
    }

    /** The upgrade name of the trap, e.g. {@code ALARMTRAP} or the name of an addon trap. */
    public String getTrap() {
        return trap;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
