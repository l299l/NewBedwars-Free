package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired before a player's armor is put on, on respawn and after buying armor. Use {@link #setArmor} to
 * change it.
 */
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

    /** Boots, leggings, chestplate and helmet, the order of {@code setArmorContents}. */
    public ItemStack[] getArmor() {
        return armor;
    }

    /**
     * @param armor exactly 4 entries in the order of {@link #getArmor()}; {@code null} entries leave a slot empty
     * @throws IllegalArgumentException when the array doesn't have 4 entries
     */
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
