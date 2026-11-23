package com.l299l.newbedwars.arena.shops.customitems;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.NoneLogic;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomItemOfferBuyTest {
    private Player player;
    private PlayerInventory inventory;
    private ItemStack given;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getMessages()).thenReturn(mock(Messages.class));
        when(plugin.getGuiManager()).thenReturn(mock(GUIManager.class));
        NewBedwars.plugin = plugin;
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getWorld()).thenReturn(mock(World.class));
        when(player.getName()).thenReturn("Steve");
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("Arena1");
        when(arena.status()).thenReturn(GameStatus.playing);
        when(arena.isPlayerInArena(player)).thenReturn(true);
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        ArenaRegistry.bindWorld(player.getWorld(), arena);
        given = mock(ItemStack.class);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
        ArenaRegistry.clear();
    }

    private CustomItem wool() {
        CustomItem item = spy(new CustomItem("Wool", "default", Material.WHITE_WOOL, 4, PriceType.IRON, 16, "default", "",
                new NoneLogic(), OnBuyEvent.GIVE_ITEM, false, new HashMap<>(), new ArrayList<>(), true));
        doReturn(given).when(item).getItem(any(Player.class), anyInt());
        return item;
    }

    @Test
    void offerChargesItsOwnPriceCurrencyAndGivesItsAmount() {
        CustomItem item = wool();
        when(inventory.contains(Material.GOLD_INGOT, 2)).thenReturn(true);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            assertEquals(PurchaseResult.BOUGHT, item.buyItem(player, new ItemPricing.Offer(2, PriceType.GOLD, null, 5)));
        }
        ArgumentCaptor<ItemStack> paid = ArgumentCaptor.forClass(ItemStack.class);
        verify(inventory).removeItem(paid.capture());
        assertEquals(Material.GOLD_INGOT, paid.getValue().getType());
        assertEquals(2, paid.getValue().getAmount());
        verify(item).getItem(player, 5);
        verify(inventory).addItem(given);
    }

    @Test
    void offerPriceIsCheckedNotTheItemsOwn() {
        CustomItem item = wool();
        when(inventory.contains(Material.IRON_INGOT, 4)).thenReturn(true);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            assertEquals(PurchaseResult.NOT_ENOUGH, item.buyItem(player, new ItemPricing.Offer(9, PriceType.GOLD, null, 5)));
        }
        verify(inventory, never()).removeItem(any(ItemStack[].class));
        verify(inventory, never()).addItem(any(ItemStack[].class));
    }
}
