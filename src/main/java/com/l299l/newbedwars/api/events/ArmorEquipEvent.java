package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/** Fired before a player's armor is applied (respawn, armor purchase). Change {@link #setArmor} to alter it. */
public class ArmorEquipEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private ItemStack[] armor;

    public ArmorEquipEvent(ArenaView arena, Player player, ItemStack[] armor) {
        super(arena);
        this.player = player;
        this.armor = armor;
    }

    public Player getPlayer() {
        return player;
    }

    /** Armor about to be set: boots, leggings, chestplate, helmet (the order of {@code setArmorContents}). */
    public ItemStack[] getArmor() {
        return armor;
    }

    public void setArmor(ItemStack[] armor) {
        if (armor == null || armor.length != 4) throw new IllegalArgumentException("armor must have 4 slots");
        this.armor = armor;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
