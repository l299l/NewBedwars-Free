package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Fired when a player picks a team in the waiting lobby of {@link #getArena()}, or clears the pick. Cancelling it
 * ignores the pick.
 */
public class TeamPickEvent extends CancellableArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final TeamView team;
    private final TeamView previousTeam;
    private final List<Player> movedPlayers;

    public TeamPickEvent(ArenaView arena, Player player, TeamView team, TeamView previousTeam, List<Player> movedPlayers) {
        super(arena);
        this.player = player;
        this.team = team;
        this.previousTeam = previousTeam;
        this.movedPlayers = Collections.unmodifiableList(new ArrayList<>(movedPlayers));
    }

    public Player getPlayer() {
        return player;
    }

    /** The picked team, or empty when the pick is cleared and the team is chosen automatically at the start. */
    public Optional<TeamView> getTeam() {
        return Optional.ofNullable(team);
    }

    /** The team the player was picked into before, if any. */
    public Optional<TeamView> getPreviousTeam() {
        return Optional.ofNullable(previousTeam);
    }

    /** Every player this pick applies to: the picker, plus the whole party when the party leader picks. */
    public List<Player> getMovedPlayers() {
        return movedPlayers;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
