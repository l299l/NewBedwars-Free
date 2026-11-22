package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired after a player respawned at their team spawn (after the respawn countdown), with their armor,
 * permanent items and team upgrades already given back.
 *
 * @since 2.8-beta
 */
public class PlayerRespawnArenaEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final TeamView team;

    public PlayerRespawnArenaEvent(ArenaView arena, Player player, TeamView team) {
        super(arena);
        this.player = player;
        this.team = team;
    }

    public Player getPlayer() {
        return player;
    }

    public TeamView getTeam() {
        return team;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
