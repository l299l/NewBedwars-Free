package com.l299l.newbedwars.gui.configuration.game.guis.other;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public record GuiCategory(String id, String name, CustomItem icon, List<Object> items, String description) {
    public ItemStack getIcon(Player player) {
        ItemStack item = icon.getIcon(player);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (description != null && !description.isEmpty()) {
                meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', description).split(";|\n")));
            } else {
                meta.setLore(Collections.emptyList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
