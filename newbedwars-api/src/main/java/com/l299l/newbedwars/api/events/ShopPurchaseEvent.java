package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired before a shop item, armor, tool or team upgrade is paid for. Cancelling it stops the purchase and
 * nothing is charged. {@link #setPrice(int)} changes what the player pays (from 2.8-beta).
 */
public class ShopPurchaseEvent extends CancellableArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String itemName;
    private final Kind kind;
    private final String priceType;
    private final TeamView team;
    private int price;

    public ShopPurchaseEvent(ArenaView arena, Player player, String itemName, Kind kind, String priceType, int price) {
        this(arena, player, itemName, kind, priceType, price, null);
    }

    /** @since 2.8-beta */
    public ShopPurchaseEvent(ArenaView arena, Player player, String itemName, Kind kind, String priceType, int price,
                             TeamView team) {
        super(arena);
        this.player = player;
        this.itemName = itemName;
        this.kind = kind;
        this.priceType = priceType;
        this.price = price;
        this.team = team;
    }

    /**
     * The buyer's team; empty when the player has no team.
     *
     * @since 2.8-beta
     */
    public java.util.Optional<TeamView> getTeam() {
        return java.util.Optional.ofNullable(team);
    }

    /**
     * Changes the price the player pays, in the same currency ({@link #getPriceType()}); 0 makes it free.
     * The event is only fired when the player can pay the normal price, so a discount can't make an item
     * affordable; a higher price the player can't pay fails like any purchase without enough resources.
     *
     * @throws IllegalArgumentException for a negative price
     * @since 2.8-beta
     */
    public void setPrice(int price) {
        if (price < 0) throw new IllegalArgumentException("price must not be negative");
        this.price = price;
    }

    public Player getPlayer() {
        return player;
    }

    /** The item's name in items.yml, or the upgrade's name for {@link Kind#UPGRADE}. */
    public String getItemName() {
        return itemName;
    }

    public Kind getKind() {
        return kind;
    }

    /**
     * {@code IRON}, {@code GOLD}, {@code DIAMOND} or {@code EMERALD}. Free items use {@code NONE}; items with
     * {@code priceType: OTHER} give the material they cost, e.g. {@code NETHER_STAR}.
     */
    public String getPriceType() {
        return priceType;
    }

    /** How many of the currency the purchase costs (after {@link #setPrice(int)}). */
    public int getPrice() {
        return price;
    }

    public enum Kind {
        /** An item that goes into the inventory. */
        ITEM,
        /** An armor tier. */
        ARMOR,
        /** A sword or a tool that follows the tool rules. */
        TOOL,
        /** A team upgrade or trap. */
        UPGRADE
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
