package com.l299l.newbedwars.api.registry;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import org.bukkit.entity.Player;

/**
 * What a special shop item does when a player right-clicks with it. Register it with
 * {@link NewBedwarsAPI#registerShopLogic}, then give a shop item that key in items.yml
 * ({@code itemLogic: YOUR_KEY}).
 */
@FunctionalInterface
public interface ShopLogic {
    /**
     * Runs on the server thread.
     *
     * @return {@code true} to use up one item, {@code false} to keep it
     */
    boolean perform(Player player, ArenaView arena);
}
