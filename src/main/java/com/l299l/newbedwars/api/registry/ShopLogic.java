package com.l299l.newbedwars.api.registry;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import org.bukkit.entity.Player;

/**
 * What a special shop item does when its owner right-clicks with it. Register it with
 * {@link NewBedwarsAPI#registerShopLogic}, then give a shop item that logic key in {@code items.yml}
 * ({@code itemLogic: YOUR_KEY}).
 */
@FunctionalInterface
public interface ShopLogic {
    /** @return true to consume one item, false to keep it */
    boolean perform(Player player, ArenaView arena);
}
