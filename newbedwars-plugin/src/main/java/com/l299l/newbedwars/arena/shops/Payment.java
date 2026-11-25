package com.l299l.newbedwars.arena.shops;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public final class Payment {
    private Payment() {
    }

    public static boolean has(Player player, Material material, int amount) {
        if (amount <= 0) return true;
        PlayerInventory inventory = player.getInventory();
        if (inventory.contains(material, amount)) return true;
        int total = 0;
        ItemStack[] storage = inventory.getStorageContents();
        if (storage != null) {
            for (ItemStack stack : storage) {
                if (stack != null && stack.getType() == material) total += stack.getAmount();
            }
        }
        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand != null && offHand.getType() == material) total += offHand.getAmount();
        return total >= amount;
    }

    public static void take(Player player, Material material, int amount) {
        if (amount <= 0) return;
        PlayerInventory inventory = player.getInventory();
        Map<Integer, ItemStack> left = inventory.removeItem(new ItemStack(material, amount));
        if (left == null || left.isEmpty()) return;
        int remaining = 0;
        for (ItemStack stack : left.values()) remaining += stack.getAmount();
        ItemStack offHand = inventory.getItemInOffHand();
        if (remaining <= 0 || offHand == null || offHand.getType() != material) return;
        int take = Math.min(remaining, offHand.getAmount());
        if (take >= offHand.getAmount()) {
            inventory.setItemInOffHand(null);
        } else {
            offHand.setAmount(offHand.getAmount() - take);
            inventory.setItemInOffHand(offHand);
        }
    }
}
