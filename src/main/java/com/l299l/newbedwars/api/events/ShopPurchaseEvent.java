package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** Fired before a shop purchase is charged. Cancelling it makes the purchase fail without charging. */
public class ShopPurchaseEvent extends ArenaEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String itemName;
    private final Kind kind;
    private final String priceType;
    private final int price;

    public ShopPurchaseEvent(ArenaView arena, Player player, String itemName, Kind kind, String priceType, int price) {
        super(arena);
        this.player = player;
        this.itemName = itemName;
        this.kind = kind;
        this.priceType = priceType;
        this.price = price;
    }

    public Player getPlayer() {
        return player;
    }

    public String getItemName() {
        return itemName;
    }

    public Kind getKind() {
        return kind;
    }

    /** Currency name: IRON, GOLD, DIAMOND or EMERALD. */
    public String getPriceType() {
        return priceType;
    }

    public int getPrice() {
        return price;
    }

    public enum Kind {
        ITEM,
        ARMOR,
        TOOL,
        UPGRADE
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
