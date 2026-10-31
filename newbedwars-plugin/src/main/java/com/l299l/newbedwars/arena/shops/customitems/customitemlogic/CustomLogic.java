package com.l299l.newbedwars.arena.shops.customitems.customitemlogic;

import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.Player;

public interface CustomLogic {
    /**
     * Performs the custom item logic.
     * @param player the player using the item
     * @param arena the arena
     * @return true if the action succeeded and the item should be consumed, false otherwise
     */
    boolean perform(Player player, IArena arena);
    LogicType getType();

    /** Key saved in items.yml: the built-in type name, or the key an addon registered the logic with. */
    default String getKey() {
        return getType().name();
    }
}
