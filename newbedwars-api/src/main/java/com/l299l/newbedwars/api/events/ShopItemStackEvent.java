package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Fired when NewBedwars builds the item stack of a shop item for one player: the icon shown in the shop, upgrade
 * or quick-buy menu ({@link Use#ICON}), and the item the player gets when buying it or when a permanent item is given
 * back after a respawn ({@link Use#GIVEN}). Addons can change or replace the stack, e.g. give the red team mangrove
 * planks instead of oak planks, and the shop shows the same planks.
 * <p>
 * Keep the item meta when replacing the stack: it holds the name, the description and the key NewBedwars uses to
 * recognise its items. For example:
 * <pre>{@code
 * ItemStack planks = new ItemStack(Material.MANGROVE_PLANKS, e.getItem().getAmount());
 * planks.setItemMeta(e.getItem().getItemMeta());
 * e.setItem(planks);
 * }</pre>
 * Wool, glass, terracotta and the other team-colored blocks are already in the team color. Not an
 * {@link ArenaEvent}: icons are also built outside arenas, e.g. in the quick-buy editor.
 *
 * @since 2.8-beta
 */
public class ShopItemStackEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String itemName;
    private final Use use;
    private final ArenaView arena;
    private final TeamView team;
    private ItemStack item;

    /** What the stack is for. */
    public enum Use {
        /** The icon in a menu; it shows the shop name and description. */
        ICON,
        /** The item the player gets. */
        GIVEN
    }

    public ShopItemStackEvent(Player player, String itemName, Use use, ItemStack item, ArenaView arena, TeamView team) {
        this.player = player;
        this.itemName = itemName;
        this.use = use;
        this.item = item;
        this.arena = arena;
        this.team = team;
    }

    /** The player who sees the icon or gets the item. */
    public Player getPlayer() {
        return player;
    }

    /** The name of the shop item in items.yml. */
    public String getItemName() {
        return itemName;
    }

    public Use getUse() {
        return use;
    }

    /** The stack NewBedwars built. It can be changed in place. */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Replaces the stack.
     *
     * @throws IllegalArgumentException for null or air
     */
    public void setItem(ItemStack item) {
        if (item == null || item.getType().isAir()) throw new IllegalArgumentException("the item must not be null or air");
        this.item = item;
    }

    /** The arena the player is in; empty outside arenas. */
    public Optional<ArenaView> getArena() {
        return Optional.ofNullable(arena);
    }

    /** The player's team; empty before the teams are assigned and outside arenas. */
    public Optional<TeamView> getTeam() {
        return Optional.ofNullable(team);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
