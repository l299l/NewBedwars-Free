package com.l299l.newbedwars.apiimpl.adapter;

import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.api.registry.ShopLogic;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import org.bukkit.entity.Player;

/** Runs an addon's {@link ShopLogic} as shop item logic. */
public final class ApiShopLogic implements CustomLogic {
    private final ShopLogic logic;

    public ApiShopLogic(ShopLogic logic) {
        this.logic = logic;
    }

    @Override
    public boolean perform(Player player, IArena arena) {
        return logic.perform(player, new ApiArena(arena));
    }

    @Override
    public LogicType getType() {
        return LogicType.NONE;
    }
}
