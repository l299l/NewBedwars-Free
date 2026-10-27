package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.ArenaView;
import com.l299l.newbedwars.api.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** Fired before a team upgrade or trap is bought. Cancelling it makes the purchase fail without charging. */
public class TeamUpgradeEvent extends ArenaEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final TeamView team;
    private final String upgrade;
    private final int newLevel;
    private final Player buyer;

    public TeamUpgradeEvent(ArenaView arena, TeamView team, String upgrade, int newLevel, Player buyer) {
        super(arena);
        this.team = team;
        this.upgrade = upgrade;
        this.newLevel = newLevel;
        this.buyer = buyer;
    }

    public TeamView getTeam() {
        return team;
    }

    /** Upgrade or trap name, e.g. SHARPNESS or ALARMTRAP. */
    public String getUpgrade() {
        return upgrade;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public Player getBuyer() {
        return buyer;
    }

    private boolean cancelled;

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
