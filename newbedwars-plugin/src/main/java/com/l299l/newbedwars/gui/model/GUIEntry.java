package com.l299l.newbedwars.gui.model;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Something a YAML shop GUI can show on a slot: a shop item, a category or an upgrade. */
public interface GUIEntry {
    ItemStack getIcon(Player player);
}
