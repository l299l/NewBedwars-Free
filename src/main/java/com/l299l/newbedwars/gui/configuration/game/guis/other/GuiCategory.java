package com.l299l.newbedwars.gui.configuration.game.guis.other;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public record GuiCategory(String id, CustomItem icon, List<Object> items) {
    public ItemStack getIcon(Player player) {
        return icon.getIcon(NewBedwars.plugin.getPlayerManager().getPlayer(player.getName()).language());
    }
}
