package com.l299l.newbedwars.arena.shops.customitems.customitemlogic;

import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.Player;

public interface CustomLogic {
    void perform(Player player, IArena arena);
    LogicType getType();
}
