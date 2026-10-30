package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.command.AddonCommand;
import com.l299l.newbedwars.api.registry.ArmorTierDefinition;
import com.l299l.newbedwars.api.command.CommandContext;
import com.l299l.newbedwars.api.registry.UpgradeDefinition;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.phases.PhaseAction;
import com.l299l.newbedwars.arena.phases.PhaseActionRegistry;
import com.l299l.newbedwars.arena.player.inventory.ArmorTier;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogicRegistry;
import com.l299l.newbedwars.commands.bedwars.MainCommand;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApiRegistrationsTest {
    private NewBedwarsApiImpl api;
    private Plugin addon;
    private MainCommand main;
    private CustomItemManager items;

    @BeforeEach
    void setUp() {
        api = new NewBedwarsApiImpl();
        addon = mock(Plugin.class);
        when(addon.getName()).thenReturn("MyAddon");
        main = mock(MainCommand.class);
        items = mock(CustomItemManager.class);
        NewBedwars.plugin = mock(NewBedwars.class);
        when(NewBedwars.plugin.getMainCommand()).thenReturn(main);
        when(NewBedwars.plugin.getCustomItemManager()).thenReturn(items);
    }

    @AfterEach
    void tearDown() {
        api.unregisterAll(addon);
        NewBedwars.plugin = null;
    }

    @Test
    void phaseActionRunsTheAddonHandlerWithAnArenaView() {
        List<String> calls = new ArrayList<>();
        api.registerPhaseAction(addon, "MY_PHASE", (arena, arg) -> calls.add(arena.getName() + "/" + arg));
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("A1");
        new PhaseAction("MY_PHASE:go").execute(arena);
        assertEquals(List.of("A1/go"), calls);
        assertThrows(IllegalArgumentException.class, () -> api.registerPhaseAction(addon, "my_phase", (a, s) -> {}));
    }

    @Test
    void commandIsRegisteredInTheRightContext() {
        AddonCommand command = mock(AddonCommand.class);
        when(command.getName()).thenReturn("cw");
        api.registerCommand(addon, command, CommandContext.GAME_ADMIN);
        ArgumentCaptor<SubCommand> captor = ArgumentCaptor.forClass(SubCommand.class);
        verify(main).register(captor.capture(), eq(MainCommand.Context.GAME_ADMIN));
        assertEquals("cw", captor.getValue().getName());
        Player player = mock(Player.class);
        captor.getValue().perform(player, new String[]{"cw"}, null);
        verify(command).perform(eq(player), any(), eq(null));
    }

    @Test
    void everyApiCommandContextHasAnInternalOne() {
        for (CommandContext context : CommandContext.values()) {
            assertNotNull(NewBedwarsApiImpl.toInternal(context));
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedBuildingContextMapsToAutomatic() {
        assertEquals(MainCommand.Context.SETUP_AUTOMATIC, NewBedwarsApiImpl.toInternal(CommandContext.SETUP_BUILDING));
        assertEquals(MainCommand.Context.SETUP_AUTOMATIC, NewBedwarsApiImpl.toInternal(CommandContext.SETUP_AUTOMATIC));
    }

    @Test
    void shopLogicIsResolvedByKey() {
        List<String> calls = new ArrayList<>();
        api.registerShopLogic(addon, "MY_LOGIC", (player, arena) -> { calls.add(arena.getName()); return true; });
        var logic = CustomLogicRegistry.create("my_logic");
        assertNotNull(logic);
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("A1");
        assertTrue(logic.perform(mock(Player.class), arena));
        assertEquals(List.of("A1"), calls);
    }

    @Test
    void armorTierIsRegisteredWithItsPieces() {
        api.registerArmorTier(addon, new ArmorTierDefinition("API_TIER", 20, Material.IRON_BOOTS, Material.IRON_LEGGINGS,
                Material.IRON_CHESTPLATE, Material.IRON_HELMET));
        assertTrue(ArmorTier.values().stream().anyMatch(t -> t.name().equals("API_TIER") && t.rank() == 20));
    }

    @Test
    void upgradeAndTrapAreRegisteredAndGetAShopIcon() {
        api.registerUpgrade(addon, UpgradeDefinition.upgrade("ApiThorns", 3, Material.CACTUS, List.of(2, 4, 6), (p, t, level) -> {}));
        api.registerUpgrade(addon, UpgradeDefinition.trap("ApiTrap", 1, Material.TNT, List.of(3), (e, t, a) -> {}));
        Upgrade thorns = Upgrade.valueOf("APITHORNS");
        assertEquals(3, thorns.maxLevel);
        assertEquals(List.of(2, 4, 6), thorns.defaultPrices());
        assertFalse(thorns.handler().isTrap());
        assertTrue(Upgrade.valueOf("APITRAP").handler().isTrap());
        verify(items).createCustomUpdateItem("ApiThorns", Material.CACTUS);
        verify(items).createCustomUpdateItem("ApiTrap", Material.TNT);
    }

    @Test
    void existingShopIconIsNotRecreated() {
        when(items.getCustomItem("ApiKeep")).thenReturn(mock(com.l299l.newbedwars.arena.shops.customitems.CustomItem.class));
        api.registerUpgrade(addon, UpgradeDefinition.upgrade("ApiKeep", 1, Material.CACTUS, List.of(1), (p, t, level) -> {}));
        verify(items, never()).createCustomUpdateItem(anyString(), any());
    }

    @Test
    void upgradeNeedsAnEffectOrATrap() {
        assertThrows(IllegalArgumentException.class, () ->
                api.registerUpgrade(addon, UpgradeDefinition.upgrade("ApiEmpty", 1, Material.CACTUS, List.of(1), null)));
    }

    @Test
    void disablingTheAddonRemovesEverythingItRegistered() {
        api.registerPhaseAction(addon, "GONE_PHASE", (a, s) -> {});
        api.registerShopLogic(addon, "GONE_LOGIC", (p, a) -> true);
        api.registerArmorTier(addon, new ArmorTierDefinition("GONE_TIER", 21, Material.IRON_BOOTS, Material.IRON_LEGGINGS,
                Material.IRON_CHESTPLATE, Material.IRON_HELMET));
        api.registerUpgrade(addon, UpgradeDefinition.upgrade("GoneUp", 1, Material.CACTUS, List.of(1), (p, t, l) -> {}));
        AddonCommand command = mock(AddonCommand.class);
        when(command.getName()).thenReturn("gone");
        api.registerCommand(addon, command, CommandContext.LOBBY);

        try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::isPrimaryThread).thenReturn(true);
            api.onPluginDisable(new PluginDisableEvent(addon));
        }

        assertNull(PhaseActionRegistry.get("GONE_PHASE"));
        assertNull(CustomLogicRegistry.create("GONE_LOGIC"));
        assertTrue(ArmorTier.values().stream().noneMatch(t -> t.name().equals("GONE_TIER")));
        assertThrows(IllegalArgumentException.class, () -> Upgrade.valueOf("GONEUP"));
        verify(main).unregister(any(SubCommand.class));
    }

    @Test
    void anotherAddonsRegistrationsSurvive() {
        Plugin other = mock(Plugin.class);
        api.registerPhaseAction(other, "KEEP_PHASE", (a, s) -> {});
        try {
            api.unregisterAll(addon);
            assertNotNull(PhaseActionRegistry.get("KEEP_PHASE"));
        } finally {
            api.unregisterAll(other);
        }
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
