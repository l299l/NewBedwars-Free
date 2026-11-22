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
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomItemBuyActionsTest {
    private Player player;
    private PlayerInventory inventory;
    private ConsoleCommandSender console;

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
        console = mock(ConsoleCommandSender.class);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
        ArenaRegistry.clear();
    }

    private IArena bindArena(GameStatus status, boolean participant, boolean spectator) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("Arena1");
        when(arena.status()).thenReturn(status);
        when(arena.isPlayerInArena(player)).thenReturn(participant);
        when(arena.getSpectators()).thenReturn(spectator ? new ArrayList<>(List.of(player)) : new ArrayList<>());
        ArenaRegistry.bindWorld(player.getWorld(), arena);
        return arena;
    }

    private IArena bindPlayingArena() {
        return bindArena(GameStatus.playing, true, false);
    }

    private CustomItem item(OnBuyEvent onBuy, PriceType priceType, int price) {
        return new CustomItem("Reward", "default", Material.NETHER_STAR, price, priceType, 1, "default", "",
                new NoneLogic(), onBuy, false, new HashMap<>(), new ArrayList<>(), true);
    }

    @Test
    void commandsRunAfterPayingWithAnyItem() {
        CustomItem item = item(OnBuyEvent.COMMANDS, PriceType.OTHER, 3);
        item.setOtherPriceMaterial(Material.DIAMOND_BLOCK);
        item.setCommands(List.of("/give {player} apple", "player: spawn", "  "));
        when(inventory.contains(Material.DIAMOND_BLOCK, 3)).thenReturn(true);
        bindPlayingArena();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            assertEquals(PurchaseResult.BOUGHT, item.buyItem(player));
            bukkit.verify(() -> Bukkit.dispatchCommand(console, "give Steve apple"));
        }
        verify(player).performCommand("spawn");
        org.mockito.ArgumentCaptor<ItemStack> paid = org.mockito.ArgumentCaptor.forClass(ItemStack.class);
        verify(inventory).removeItem(paid.capture());
        assertEquals(Material.DIAMOND_BLOCK, paid.getValue().getType());
        assertEquals(3, paid.getValue().getAmount());
    }

    @Test
    void otherPriceWithoutMaterialIsNeverFree() {
        CustomItem item = item(OnBuyEvent.COMMANDS, PriceType.OTHER, 3);
        item.setCommands(List.of("say hi"));
        bindPlayingArena();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            assertEquals(PurchaseResult.UNAVAILABLE, item.buyItem(player));
            bukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());
        }
    }

    @Test
    void notEnoughOfTheItemBuysNothing() {
        CustomItem item = item(OnBuyEvent.COMMANDS, PriceType.OTHER, 3);
        item.setOtherPriceMaterial(Material.DIAMOND_BLOCK);
        item.setCommands(List.of("say hi"));
        bindPlayingArena();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            assertEquals(PurchaseResult.NOT_ENOUGH, item.buyItem(player));
            bukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());
        }
        verify(inventory, never()).removeItem(any(ItemStack[].class));
    }

    @Test
    void permissionNeedsAGame() {
        CustomItem item = item(OnBuyEvent.GIVE_PERM, PriceType.NONE, 0);
        item.setGivePermission("fly.use");
        assertEquals(PurchaseResult.UNAVAILABLE, item.buyItem(player));
        verify(player, never()).addAttachment(any());
    }

    @Test
    void nothingIsBoughtOutsideAPlayingGameAsAParticipant() {
        CustomItem item = item(OnBuyEvent.COMMANDS, PriceType.NONE, 0);
        item.setCommands(List.of("say hi"));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            assertEquals(PurchaseResult.UNAVAILABLE, item.buyItem(player));
            bindArena(GameStatus.waiting, true, false);
            assertEquals(PurchaseResult.UNAVAILABLE, item.buyItem(player));
            bindArena(GameStatus.ending, true, false);
            assertEquals(PurchaseResult.UNAVAILABLE, item.buyItem(player));
            bindArena(GameStatus.playing, false, false);
            assertEquals(PurchaseResult.UNAVAILABLE, item.buyItem(player));
            bindArena(GameStatus.playing, true, true);
            assertEquals(PurchaseResult.UNAVAILABLE, item.buyItem(player));
            bukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());
        }
    }

    @Test
    void unknownMenuIsNotBought() {
        CustomItem item = item(OnBuyEvent.OPEN_GUI, PriceType.NONE, 0);
        item.setOpenGui("missing");
        bindPlayingArena();
        assertEquals(PurchaseResult.UNAVAILABLE, item.buyItem(player));
        verify(player, never()).openInventory(any(org.bukkit.inventory.Inventory.class));
    }

    @Test
    void reasonsForOtherFailures() {
        CustomItem vip = new CustomItem("Vip", "default", Material.STONE, 1, PriceType.IRON, 1, "default", "vip.buy",
                new NoneLogic(), OnBuyEvent.GIVE_ITEM, false, new HashMap<>(), new ArrayList<>(), true);
        assertEquals(PurchaseResult.NO_PERMISSION, vip.buyItem(player));
        assertEquals(PurchaseResult.CANCELLED, item(OnBuyEvent.CANCEL, PriceType.NONE, 0).buyItem(player));
        bindPlayingArena();
        assertEquals(PurchaseResult.UNAVAILABLE, item(OnBuyEvent.GIVE_ARMOR, PriceType.IRON, 1).buyItem(player));
        assertEquals("NotEnoughMoney", PurchaseResult.NOT_ENOUGH.messageKey());
        assertEquals("ShopAlreadyOwned", PurchaseResult.ALREADY_OWNED.messageKey());
        assertNull(PurchaseResult.CANCELLED.messageKey());
    }

    @Test
    void permissionAlreadyBoughtIsReported() {
        World world = player.getWorld();
        bindPlayingArena();
        org.bukkit.plugin.Plugin plugin = NewBedwars.plugin;
        when(plugin.isEnabled()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(player.addAttachment(plugin)).thenAnswer(i -> new org.bukkit.permissions.PermissionAttachment(plugin, player));
        try {
            CustomItem item = item(OnBuyEvent.GIVE_PERM, PriceType.NONE, 0);
            item.setGivePermission("fly.use");
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getPluginManager).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
                assertEquals(PurchaseResult.BOUGHT, item.buyItem(player));
                assertEquals(PurchaseResult.ALREADY_OWNED, item.buyItem(player));
            }
        } finally {
            GamePermissions.revokeAll();
            com.l299l.newbedwars.arena.ArenaRegistry.clear();
        }
    }
}
