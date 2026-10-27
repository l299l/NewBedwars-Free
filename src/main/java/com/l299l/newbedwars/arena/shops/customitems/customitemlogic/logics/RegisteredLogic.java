package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogicRegistry;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import org.bukkit.entity.Player;

/**
 * Stand-in for logic registered by an addon. It only remembers the key (so the item is saved with it) and
 * looks the real logic up when the item is used; with the addon missing, using the item does nothing.
 */
public class RegisteredLogic implements CustomLogic {
    private final String key;

    public RegisteredLogic(String key) {
        this.key = CustomLogicRegistry.normalize(key);
    }

    @Override
    public boolean perform(Player player, IArena arena) {
        CustomLogic logic = CustomLogicRegistry.create(key);
        return logic != null && logic.perform(player, arena);
    }

    @Override
    public LogicType getType() {
        return LogicType.NONE;
    }

    @Override
    public String getKey() {
        return key;
    }
}
