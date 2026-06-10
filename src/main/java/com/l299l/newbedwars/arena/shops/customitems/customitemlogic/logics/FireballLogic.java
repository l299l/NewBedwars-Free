package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;

public class FireballLogic implements CustomLogic {

    @Override
    public void perform(Player player, IArena arena) {
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setDirection(player.getEyeLocation().getDirection().multiply(2));
        fireball.setYield(2.0f);
        fireball.setIsIncendiary(false);
        fireball.setShooter(player);
    }

    @Override
    public LogicType getType() {
        return LogicType.FIREBALL;
    }
}
