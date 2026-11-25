package com.l299l.newbedwars.arena.shops;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentTest {
    private final Player player = mock(Player.class);
    private final PlayerInventory inventory = mock(PlayerInventory.class);

    PaymentTest() {
        when(player.getInventory()).thenReturn(inventory);
    }

    private static HashMap<Integer, ItemStack> left(int amount) {
        HashMap<Integer, ItemStack> map = new HashMap<>();
        map.put(0, new ItemStack(Material.IRON_INGOT, amount));
        return map;
    }

    @Test
    void takesTheRestFromTheOffHandWhenTheStorageIsShort() {
        ItemStack offHand = new ItemStack(Material.IRON_INGOT, 5);
        when(inventory.removeItem(any(ItemStack[].class))).thenReturn(left(3));
        when(inventory.getItemInOffHand()).thenReturn(offHand);
        Payment.take(player, Material.IRON_INGOT, 10);
        assertEquals(2, offHand.getAmount());
        verify(inventory).setItemInOffHand(offHand);
    }

    @Test
    void emptiesTheOffHandWhenItIsUsedUp() {
        when(inventory.removeItem(any(ItemStack[].class))).thenReturn(left(4));
        when(inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.IRON_INGOT, 4));
        Payment.take(player, Material.IRON_INGOT, 10);
        verify(inventory).setItemInOffHand(null);
    }

    @Test
    void leavesTheOffHandAloneWhenTheStorageCoversThePrice() {
        when(inventory.removeItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        Payment.take(player, Material.IRON_INGOT, 10);
        verify(inventory, never()).setItemInOffHand(any());
    }

    @Test
    void hasCountsStorageAndOffHandTogether() {
        when(inventory.contains(Material.IRON_INGOT, 10)).thenReturn(false);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{new ItemStack(Material.IRON_INGOT, 4), null});
        when(inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.IRON_INGOT, 6));
        org.junit.jupiter.api.Assertions.assertTrue(Payment.has(player, Material.IRON_INGOT, 10));
        org.junit.jupiter.api.Assertions.assertFalse(Payment.has(player, Material.IRON_INGOT, 11));
    }
}
