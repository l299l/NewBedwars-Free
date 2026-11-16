package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired after a player's arena or team chat message was delivered. It can't be cancelled; to block messages,
 * listen to Bukkit's chat event instead.
 *
 * @since 2.7-beta
 */
public class ArenaChatEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String message;
    private final boolean teamChat;
    private final TeamView team;

    public ArenaChatEvent(ArenaView arena, Player player, String message, boolean teamChat, TeamView team) {
        super(arena);
        this.player = player;
        this.message = message;
        this.teamChat = teamChat;
        this.team = team;
    }

    public Player getPlayer() {
        return player;
    }

    /** The message without color codes. */
    public String getMessage() {
        return message;
    }

    /** {@code true} for team chat ({@code !message}), {@code false} for arena chat. */
    public boolean isTeamChat() {
        return teamChat;
    }

    /** The player's team, or {@code null} for spectators and in the waiting lobby. */
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
