package com.l299l.newbedwars.arena.shops.upgrades;

import com.l299l.newbedwars.api.events.ShopPurchaseEvent;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UpgradePurchaseTest {
    @TempDir
    Path dir;
    private MockedStatic<Bukkit> bukkit;
    private PluginManager pm;
    private Player player;
    private PlayerInventory inventory;
    private Team team;
    private TeamUpgrades upgrades;
    private IArena arena;
    private UpgradePricing pricing;
    private int level;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        pm = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        team = mock(Team.class);
        upgrades = mock(TeamUpgrades.class);
        when(team.getTeamUpgrades()).thenReturn(upgrades);
        when(upgrades.getUpgradeLevel(any())).thenAnswer(i -> level);
        doAnswer(i -> level++).when(upgrades).upgrade(any());
        arena = mock(IArena.class);
        pricing = new UpgradePricing(dir, Runnable::run, Logger.getLogger("test"), 3);
        pricing.load(null);
        pricing.set("HASTE", List.of(new UpgradeTier(2, PriceType.IRON), new UpgradeTier(3, PriceType.EMERALD)));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private UpgradePurchase.Result buy() {
        return UpgradePurchase.buy(player, arena, team, Upgrade.HASTE, "Haste", pricing, null);
    }

    @Test
    void chargesTheCurrencyOfEachLevel() {
        when(inventory.contains(any(Material.class), anyInt())).thenReturn(true);
        assertTrue(buy().bought());
        UpgradePurchase.Result second = buy();
        assertTrue(second.bought());
        assertEquals(PriceType.EMERALD, second.tier().currency());
        assertEquals(UpgradePurchase.Outcome.MAXED, buy().outcome());
        assertEquals(2, level);
        ArgumentCaptor<ItemStack> taken = ArgumentCaptor.forClass(ItemStack.class);
        verify(inventory, times(2)).removeItem(taken.capture());
        assertEquals(Material.IRON_INGOT, taken.getAllValues().get(0).getType());
        assertEquals(2, taken.getAllValues().get(0).getAmount());
        assertEquals(Material.EMERALD, taken.getAllValues().get(1).getType());
        assertEquals(3, taken.getAllValues().get(1).getAmount());
    }

    @Test
    void notEnoughMoneyChangesNothing() {
        when(inventory.contains(Material.IRON_INGOT, 2)).thenReturn(false);
        UpgradePurchase.Result result = buy();
        assertEquals(UpgradePurchase.Outcome.NOT_ENOUGH, result.outcome());
        assertEquals(2, result.tier().price());
        verify(inventory, never()).removeItem(any(ItemStack[].class));
        verify(upgrades, never()).upgrade(any());
    }

    @Test
    void cancelledEventChangesNothing() {
        when(inventory.contains(any(Material.class), anyInt())).thenReturn(true);
        doAnswer(i -> {
            Event e = i.getArgument(0);
            if (e instanceof ShopPurchaseEvent purchase) purchase.setCancelled(true);
            return null;
        }).when(pm).callEvent(any());
        assertEquals(UpgradePurchase.Outcome.CANCELLED, buy().outcome());
        verify(upgrades, never()).upgrade(any());
        verify(inventory, never()).removeItem(any(ItemStack[].class));
    }

    @Test
    void freeLevelNeedsNoItems() {
        pricing.set("HASTE", List.of(new UpgradeTier(0, PriceType.GOLD)));
        when(inventory.contains(eq(Material.GOLD_INGOT), eq(0))).thenReturn(true);
        assertTrue(buy().bought());
        verify(inventory, never()).removeItem(any(ItemStack[].class));
    }
}
