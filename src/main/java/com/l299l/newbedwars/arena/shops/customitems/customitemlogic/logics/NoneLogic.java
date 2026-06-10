package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import org.bukkit.entity.Player;

public class NoneLogic implements CustomLogic {
    @Override
    public void perform(Player player, IArena arena) {
    }

    @Override
    public LogicType getType() {
        return LogicType.NONE;
    }
}
