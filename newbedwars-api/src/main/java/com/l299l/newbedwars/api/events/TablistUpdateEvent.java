package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;

/**
 * Fired just before NewBedwars sends a changed tab list (header, footer or list name) to a player. It is not fired
 * while nothing changed. Change the texts with the setters; whatever is set afterwards is sent. Cancelling it sends
 * nothing for this change; the next change fires it again, or use
 * {@link com.l299l.newbedwars.api.tablist.TablistService#refresh(Player)}.
 *
 * @since 2.8-beta
 */
public class TablistUpdateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ArenaView arena;
    private final String layout;
    private String header;
    private String footer;
    private String playerListName;
    private boolean cancelled;

    public TablistUpdateEvent(Player player, ArenaView arena, String layout, String header, String footer, String playerListName) {
        this.player = player;
        this.arena = arena;
        this.layout = layout;
        this.header = header;
        this.footer = footer;
        this.playerListName = playerListName;
    }

    /** The player whose tab list changes. */
    public Player getPlayer() {
        return player;
    }

    /** The arena the player plays or spectates in, empty in the lobby. */
    public Optional<ArenaView> getArena() {
        return Optional.ofNullable(arena);
    }

    /** The layout used, or {@code null} for the lobby tab list. */
    public String getLayout() {
        return layout;
    }

    /** The header, lines joined by {@code \n}, colors already translated. Empty for none. */
    public String getHeader() {
        return header;
    }

    /** {@code null} counts as empty. */
    public void setHeader(String header) {
        this.header = header;
    }

    /** The footer, lines joined by {@code \n}, colors already translated. Empty for none. */
    public String getFooter() {
        return footer;
    }

    /** {@code null} counts as empty. */
    public void setFooter(String footer) {
        this.footer = footer;
    }

    /** The name shown in the player list, or {@code null} when the layout sets no {@code PlayerFormat}. */
    public String getPlayerListName() {
        return playerListName;
    }

    /** {@code null} shows the vanilla name. */
    public void setPlayerListName(String playerListName) {
        this.playerListName = playerListName;
    }

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
