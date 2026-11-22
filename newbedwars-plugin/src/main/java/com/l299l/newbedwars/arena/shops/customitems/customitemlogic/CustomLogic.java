package com.l299l.newbedwars.arena.shops.customitems.customitemlogic;

import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.Player;

public interface CustomLogic {

    boolean perform(Player player, IArena arena);

    default boolean perform(Player player, IArena arena, String itemName) {
        return perform(player, arena);
    }
    LogicType getType();

    default String getKey() {
        return getType().name();
    }
}
