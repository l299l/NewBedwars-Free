package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Fired after a chat message of a player in an arena was delivered (arena chat or team chat). Not cancellable; to block messages listen to Bukkit's chat event instead. Available from 2.7-beta. */
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

    /** The player who wrote the message. */
    public Player getPlayer() {
        return player;
    }

    /** The message without colour codes. */
    public String getMessage() {
        return message;
    }

    /** {@code true} for team chat ({@code !message}), {@code false} for the whole arena. */
    public boolean isTeamChat() {
        return teamChat;
    }

    /** The player's team, or {@code null} (spectators, waiting lobby). */
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
