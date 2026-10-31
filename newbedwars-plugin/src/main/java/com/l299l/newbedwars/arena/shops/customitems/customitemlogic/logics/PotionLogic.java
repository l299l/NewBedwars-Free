package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import org.bukkit.entity.Player;

public class PotionLogic implements CustomLogic {
    @Override
    public boolean perform(Player player, IArena arena) {
        return true; // Potion logic handled by shop GUI directly
    }

    @Override
    public LogicType getType() {
        return LogicType.POTION;
    }
}
