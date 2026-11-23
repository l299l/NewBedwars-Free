package com.l299l.newbedwars.arena.shops.editor;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.shop.ItemTexts;
import com.l299l.newbedwars.api.shop.ShopCategory;
import com.l299l.newbedwars.api.shop.ShopGui;
import com.l299l.newbedwars.api.shop.ShopItem;
import com.l299l.newbedwars.api.shop.ShopResult.Code;
import com.l299l.newbedwars.api.shop.UpgradePrice;
import com.l299l.newbedwars.api.shop.UpgradeTier;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.upgrades.UpgradePricing;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.data.DataManager;
import com.l299l.newbedwars.config.data.yaml.guis.GUIDataManager;
import com.l299l.newbedwars.config.data.yaml.guis.GuiCodec;
import com.l299l.newbedwars.config.data.yaml.items.ItemsDataManager;
import com.l299l.newbedwars.config.properties.LangMessages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUIManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShopEditServiceTest {
    @TempDir
    Path dir;
    private CustomItemManager items;
    private GUIDataManager guis;
    private ItemsDataManager itemsData;
    private UpgradePricing pricing;
    private GUIManager guiManager;
    private Messages messages;
    private final Map<Language, YamlConfiguration> langs = new EnumMap<>(Language.class);
    private String oldSword, oldShop, oldUpgrade;

    @BeforeEach
    void setUp() throws IOException {
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        messages = mock(Messages.class);
        for (Language l : Language.values()) langs.put(l, new YamlConfiguration());
        when(messages.langConf(any())).thenAnswer(i -> langs.get(i.getArgument(0)));
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getLangMessages()).thenReturn(mock(LangMessages.class));
        items = new CustomItemManager();
        when(plugin.getCustomItemManager()).thenReturn(items);
        guiManager = mock(GUIManager.class);
        when(plugin.getGuiManager()).thenReturn(guiManager);
        item("Wool", Material.WHITE_WOOL);
        item("Fill", Material.BLACK_STAINED_GLASS_PANE);
        item("WoodenSword", Material.WOODEN_SWORD);
        item("Spare", Material.STONE);
        Files.createDirectories(dir.resolve("data/guis"));
        Files.writeString(dir.resolve("data/guis/shop.yml"), """
                GuiId: shop
                Size: 9
                Fill: ci:Fill
                Slots:
                  1: cat:blocks
                  2: fill
                  3: ci:OldThing
                Categories:
                  - ID: blocks
                    Icon: Wool
                    Items: [Wool]
                """);
        guis = new GUIDataManager(dir, items::getCustomItem, Runnable::run, Logger.getLogger("test"), 3);
        guis.load();
        itemsData = new ItemsDataManager(dir, items, Runnable::run, Logger.getLogger("test"), 3);
        DataManager data = mock(DataManager.class);
        when(data.getGuiDataManager()).thenReturn(guis);
        when(data.getItemsDataManager()).thenReturn(itemsData);
        pricing = new UpgradePricing(dir, Runnable::run, Logger.getLogger("test"), 3);
        pricing.load(null);
        when(data.getUpgradePricing()).thenReturn(pricing);
        when(plugin.getDataManager()).thenReturn(data);
        oldSword = Properties.BasicSword;
        oldShop = Properties.DefaultTeamShopGui;
        oldUpgrade = Properties.DefaultUpgradeShopGui;
        Properties.BasicSword = "WoodenSword";
        Properties.DefaultTeamShopGui = "shop";
        Properties.DefaultUpgradeShopGui = "upgrades";
        ArenaRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        Properties.BasicSword = oldSword;
        Properties.DefaultTeamShopGui = oldShop;
        Properties.DefaultUpgradeShopGui = oldUpgrade;
        NewBedwars.plugin = null;
    }

    private void item(String name, Material material) {
        items.createCustomItem(name, "default", material, 1, PriceType.IRON, 1, "default", "", "NONE",
                OnBuyEvent.GIVE_ITEM, false, new HashMap<>(), new ArrayList<>(), true);
    }

    private ShopItem spec(String name, String material) {
        return new ShopItem(name, material, 4, 12, "GOLD", "GIVE_ITEM", "TNT", false, "", Map.of(), List.of("HIDE_ENCHANTS"), List.of());
    }

    private ShopGui gui(String id, List<String> slots, List<ShopCategory> categories) {
        return new ShopGui(id, "&aTitle", 9, 0, 0, true, "ci:Fill", "empty", slots, categories, List.of(), List.of());
    }

    private static List<String> slots(String... first) {
        List<String> s = new ArrayList<>(Collections.nCopies(9, "empty"));
        for (int i = 0; i < first.length; i++) s.set(i, first[i]);
        return s;
    }

    @Test
    void createsAndChangesItemsLive() throws IOException {
        assertEquals(Code.OK, ShopEditService.saveItem(spec("Bomb", "TNT")).code());
        assertEquals(Material.TNT, items.getCustomItem("bomb").getMaterial());
        assertEquals("TNT", items.getCustomItem("Bomb").getEvent().getKey());
        assertEquals("Bomb", langs.get(Language.English).getString("CustomItemsNames.Bomb-name"));
        assertTrue(Files.readString(dir.resolve("data/items.yml")).contains("Bomb:"));

        var wool = items.getCustomItem("Wool");
        assertEquals(Code.OK, ShopEditService.saveItem(spec("Wool", "RED_WOOL")).code());
        assertSame(wool, items.getCustomItem("Wool"));
        assertEquals(Material.RED_WOOL, wool.getMaterial());
        assertEquals(12, wool.getPrice());
        assertEquals(PriceType.GOLD, wool.getPriceType());
        verify(guiManager, atLeastOnce()).refresh();
    }

    @Test
    void addonShopItemIsAddedOnceAndNeverOverwritten() {
        var api = new com.l299l.newbedwars.apiimpl.NewBedwarsApiImpl();
        var owner = mock(org.bukkit.plugin.Plugin.class);
        try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::isPrimaryThread).thenReturn(true);
            ShopItem egg = ShopItem.of("EmeraldGolemEgg", "SNOWBALL", 1, 24, "IRON", "NONE");
            assertTrue(api.registerShopItem(owner, egg, Map.of("en", new ItemTexts("&fEmerald Golem", "", ""))));
            var created = items.getCustomItem("EmeraldGolemEgg");
            assertEquals(Material.SNOWBALL, created.getMaterial());
            assertEquals(24, created.getPrice());
            assertEquals(PriceType.IRON, created.getPriceType());
            assertEquals("&fEmerald Golem", langs.get(Language.English).getString("CustomItemsNames.EmeraldGolemEgg-name"));

            ShopEditService.saveItem(new ShopItem("EmeraldGolemEgg", "SNOWBALL", 1, 50, "GOLD", "GIVE_ITEM", "NONE", false, "", null, null, null));
            assertFalse(api.registerShopItem(owner, egg));
            assertEquals(50, items.getCustomItem("EmeraldGolemEgg").getPrice());
            assertFalse(api.registerShopItem(owner, ShopItem.of("Wool", "STONE", 1, 1, "IRON", "NONE")));
            assertEquals(Material.WHITE_WOOL, items.getCustomItem("Wool").getMaterial());
        }
    }

    @Test
    void addonShopItemWithInvalidValuesThrowsAndAddsNothing() {
        var api = new com.l299l.newbedwars.apiimpl.NewBedwarsApiImpl();
        var owner = mock(org.bukkit.plugin.Plugin.class);
        try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::isPrimaryThread).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () -> api.registerShopItem(owner, ShopItem.of("Egg", "NOT_A_MATERIAL", 1, 1, "IRON", "NONE")));
            assertThrows(IllegalArgumentException.class, () -> api.registerShopItem(owner, ShopItem.of("Egg", "STONE", 1, 1, "IRON", "NO_SUCH_LOGIC")));
            assertThrows(IllegalArgumentException.class, () -> api.registerShopItem(owner, ShopItem.of("Egg", "STONE", 1, 1, "IRON", "NONE"), Map.of("xx", new ItemTexts("", "", ""))));
            assertNull(items.getCustomItem("Egg"));
            bukkit.when(org.bukkit.Bukkit::isPrimaryThread).thenReturn(false);
            assertThrows(IllegalStateException.class, () -> api.registerShopItem(owner, ShopItem.of("Egg", "STONE", 1, 1, "IRON", "NONE")));
        }
    }

    @Test
    void refusesInvalidItems() {
        assertEquals(Code.INVALID, ShopEditService.saveItem(spec("bad name", "STONE")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(spec("A", "NOT_A_MATERIAL")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(spec("A", "AIR")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(new ShopItem("A", "STONE", 0, 1, "IRON", "GIVE_ITEM", "NONE", false, "", Map.of(), List.of(), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(new ShopItem("A", "STONE", 1, 1, "COPPER", "GIVE_ITEM", "NONE", false, "", Map.of(), List.of(), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(new ShopItem("A", "STONE", 1, 1, "IRON", "GIVE_ITEM", "NOT_A_LOGIC", false, "", Map.of(), List.of(), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(new ShopItem("A", "STONE", 1, 1, "IRON", "GIVE_ITEM", "NONE", false, "a b", Map.of(), List.of(), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(new ShopItem("A", "STONE", 1, 1, "IRON", "GIVE_ITEM", "NONE", false, "", Map.of(), List.of("NOPE"), List.of())).code());
        assertNull(items.getCustomItem("A"));
    }

    private static ShopItem action(String onBuy, String priceType, List<String> commands, String gui, String perm, String priceMaterial) {
        return new ShopItem("Act", "STONE", 1, 2, priceType, onBuy, "NONE", false, "", Map.of(), List.of(), List.of(),
                commands, gui, perm, priceMaterial);
    }

    @Test
    void refusesIncompleteBuyActions() {
        assertEquals(Code.INVALID, ShopEditService.saveItem(action("COMMANDS", "IRON", List.of(), "", "", "")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(action("COMMANDS", "IRON", List.of("say a\nop x"), "", "", "")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(action("OPEN_GUI", "IRON", List.of(), "nope", "", "")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(action("GIVE_PERM", "IRON", List.of(), "", "", "")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(action("GIVE_PERM", "IRON", List.of(), "", "a b", "")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(action("GIVE_ITEM", "OTHER", List.of(), "", "", "")).code());
        assertEquals(Code.INVALID, ShopEditService.saveItem(action("GIVE_ITEM", "OTHER", List.of(), "", "", "NOT_A_THING")).code());
        assertNull(items.getCustomItem("Act"));
    }

    @Test
    void savesBuyActionsAndOtherPrice() {
        assertEquals(Code.OK, ShopEditService.saveItem(action("COMMANDS", "OTHER", List.of("say {player}", " "), "", "", "nether_star")).code());
        CustomItem saved = items.getCustomItem("Act");
        assertEquals(List.of("say {player}"), saved.getCommands());
        assertEquals(Material.NETHER_STAR, saved.getOtherPriceMaterial());
        assertEquals("NETHER_STAR", saved.priceLabel());
        ShopItem api = ShopEditService.item("Act").orElseThrow();
        assertEquals(List.of("say {player}"), api.commands());
        assertEquals("NETHER_STAR", api.priceMaterial());

        assertEquals(Code.OK, ShopEditService.saveItem(action("OPEN_GUI", "IRON", List.of(), "shop", "", "")).code());
        assertEquals("shop", items.getCustomItem("Act").getOpenGui());
        assertNull(items.getCustomItem("Act").getOtherPriceMaterial());
        assertEquals(Code.OK, ShopEditService.saveItem(action("GIVE_PERM", "IRON", List.of(), "", "fly.use", "")).code());
        assertEquals("fly.use", items.getCustomItem("Act").getGivePermission());
        assertTrue(ShopEditService.OFFERED_ON_BUY.containsAll(List.of("COMMANDS", "OPEN_GUI", "GIVE_PERM")));
    }

    @Test
    void switchingTheActionDropsTheDormantPowerData() {
        assertEquals(Code.OK, ShopEditService.saveItem(action("COMMANDS", "IRON", List.of("say hi"), "", "fly.use", "")).code());
        CustomItem saved = items.getCustomItem("Act");
        assertEquals(List.of("say hi"), saved.getCommands());
        assertEquals("", saved.getGivePermission());

        assertEquals(Code.OK, ShopEditService.saveItem(action("GIVE_PERM", "IRON", List.of("say hi"), "", "fly.use", "")).code());
        assertEquals(List.of(), saved.getCommands());
        assertEquals("fly.use", saved.getGivePermission());

        assertEquals(Code.OK, ShopEditService.saveItem(action("GIVE_ITEM", "IRON", List.of("say hi"), "", "fly.use", "")).code());
        assertEquals(List.of(), saved.getCommands());
        assertEquals("", saved.getGivePermission());
    }

    @Test
    void deleteIsRefusedWhileUsed() {
        assertEquals(Code.IN_USE, ShopEditService.deleteItem("Wool").code());
        assertEquals(Code.IN_USE, ShopEditService.deleteItem("Fill").code());
        assertEquals("config:BasicSword", ShopEditService.deleteItem("WoodenSword").detail());
        assertEquals(List.of("gui:shop"), ShopEditService.itemUsages("wool"));
        assertEquals(Code.NOT_FOUND, ShopEditService.deleteItem("Nothing").code());
        assertEquals(Code.OK, ShopEditService.deleteItem("Spare").code());
        assertNull(items.getCustomItem("Spare"));
    }

    @Test
    void textsAreWrittenPerLanguage() {
        var result = ShopEditService.saveTexts("Wool", Map.of(
                "en", new ItemTexts("", "&7Line;&7Two", "Soft"),
                "pl", new ItemTexts("&fWelna", "", "")));
        assertEquals(Code.OK, result.code());
        assertEquals("Wool", langs.get(Language.English).getString("CustomItemsNames.Wool-name"));
        assertEquals("&7Line;&7Two", langs.get(Language.English).getString("CustomItemsNames.Wool-icon-description"));
        assertEquals("&fWelna", langs.get(Language.Polish).getString("CustomItemsNames.Wool-name"));
        assertNull(langs.get(Language.Polish).getString("CustomItemsNames.Wool-icon-description"));
        assertEquals("Wool-description", items.getCustomItem("Wool").getDescription());
        verify(messages).saveLangFile(Language.English, true);
        verify(messages).saveLangFile(Language.Polish, true);
        verify(messages, never()).saveLangFile(eq(Language.German), anyBoolean());

        Map<String, ItemTexts> texts = ShopEditService.texts("Wool").orElseThrow();
        assertEquals(Language.values().length, texts.size());
        assertEquals("Soft", texts.get("en").lore());
        assertEquals("&fWelna", texts.get("pl").name());

        ShopEditService.saveTexts("Wool", Map.of("en", new ItemTexts("Wool", "", "")));
        assertEquals("shop-item-description", items.getCustomItem("Wool").getDescription());
        assertEquals(Code.INVALID, ShopEditService.saveTexts("Wool", Map.of("xx", new ItemTexts("", "", ""))).code());
        assertEquals(Code.NOT_FOUND, ShopEditService.saveTexts("Nope", Map.of("en", new ItemTexts("", "", ""))).code());
    }

    @Test
    void createsGuisAndRefusesTakenIds() throws IOException {
        var result = ShopEditService.createGui(gui("extra", slots("cat:blocks", "ci:Wool", "fill"),
                List.of(new ShopCategory("blocks", "&aBlocks", "Wool", "", List.of("Wool", "ci:Spare")))));
        assertEquals(Code.OK, result.code(), result.detail());
        assertTrue(GuiCodec.parse(Files.readString(dir.resolve("data/guis/extra.yml"))).slots().contains("ci:Wool"));
        assertNotNull(guis.get("extra"));
        assertEquals(Code.NAME_TAKEN, ShopEditService.createGui(gui("extra", slots(), List.of())).code());
        assertEquals(Code.NAME_TAKEN, ShopEditService.createGui(gui("SHOP", slots(), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.createGui(gui("bad id", slots(), List.of())).code());
    }

    @Test
    void validatesGuiReferences() {
        assertEquals(Code.INVALID, ShopEditService.createGui(gui("g", slots("ci:Nope"), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.createGui(gui("g", slots("cat:missing"), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.createGui(gui("g", slots("rubbish"), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.createGui(gui("g", slots().subList(0, 8), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.createGui(gui("g", slots(), List.of(
                new ShopCategory("a", "A", "Nope", "", List.of())))).code());
        assertEquals(Code.INVALID, ShopEditService.createGui(gui("g", slots(), List.of(
                new ShopCategory("a", "A", "Wool", "", List.of()), new ShopCategory("a", "B", "Wool", "", List.of())))).code());
        assertEquals(Code.INVALID, ShopEditService.createGui(new ShopGui("g", "T", 9, 4, 0, true, "empty", "empty",
                slots(), List.of(), List.of(), List.of())).code());
        assertEquals(Code.INVALID, ShopEditService.createGui(new ShopGui("g", "T", 9, 0, 0, true, "cat:x", "empty",
                slots(), List.of(), List.of(), List.of())).code());
    }

    @Test
    void savingKeepsBrokenReferencesThatWereAlreadyThere() {
        ShopGui current = ShopEditService.gui("shop").orElseThrow();
        assertFalse(current.problems().isEmpty());
        assertEquals(List.of("config:DefaultTeamShopGui"), current.usedBy());
        List<String> slots = new ArrayList<>(current.slots());
        slots.set(3, "ci:Wool");
        ShopGui changed = new ShopGui("shop", "New", 9, 0, 0, false, current.fill(), current.empty(), slots,
                current.categories(), List.of(), List.of());
        assertEquals(Code.OK, ShopEditService.saveGui(changed).code());
        assertEquals("New", guis.get("shop").getName());
        assertEquals("ci:OldThing", guis.get("shop").getDefinition().slots().get(2));

        slots.set(4, "ci:AnotherMissing");
        assertEquals(Code.INVALID, ShopEditService.saveGui(new ShopGui("shop", "New", 9, 0, 0, false, current.fill(),
                current.empty(), slots, current.categories(), List.of(), List.of())).code());
        assertEquals(Code.NOT_FOUND, ShopEditService.saveGui(gui("nothing", slots(), List.of())).code());
    }

    @Test
    void guiDeleteIsRefusedWhileUsed() {
        assertEquals(Code.IN_USE, ShopEditService.deleteGui("shop").code());
        ShopEditService.createGui(gui("spare", slots(), List.of()));
        assertEquals(Code.OK, ShopEditService.deleteGui("spare").code());
        assertNull(guis.get("spare"));
        assertEquals(Code.NOT_FOUND, ShopEditService.deleteGui("spare").code());
    }

    @Test
    void newItemsResolveInGuisThatWaitedForThem() {
        assertNull(guis.get("shop").getItem(2));
        ShopEditService.saveItem(spec("OldThing", "STONE"));
        assertNotNull(guis.get("shop").getItem(2));
    }

    @Test
    void upgradePricesAreListedAndSaved() {
        UpgradePrice forge = ShopEditService.upgradePrices().stream().filter(u -> u.name().equals("FORGE")).findFirst().orElseThrow();
        assertEquals(8, forge.tiers().size());
        assertEquals(new UpgradeTier(6, "DIAMOND"), forge.tiers().get(0));
        assertTrue(forge.builtIn());
        assertTrue(ShopEditService.upgradePrices().stream().noneMatch(u -> u.name().equals("CUSTOMTRAP")));

        assertEquals(Code.OK, ShopEditService.saveUpgradePrices("forge",
                List.of(new UpgradeTier(3, "iron"), new UpgradeTier(5, "EMERALD"))).code());
        assertEquals(List.of(new com.l299l.newbedwars.arena.shops.upgrades.UpgradeTier(3, PriceType.IRON),
                new com.l299l.newbedwars.arena.shops.upgrades.UpgradeTier(5, PriceType.EMERALD)), pricing.tiers(Upgrade.FORGE));
        assertEquals(Code.OK, ShopEditService.saveUpgradePrices("HEALPOOL", List.of()).code());
        assertTrue(pricing.tiers(Upgrade.HEALPOOL).isEmpty());
    }

    @Test
    void invalidUpgradePricesAreRefused() {
        assertEquals(Code.NOT_FOUND, ShopEditService.saveUpgradePrices("NOPE", List.of()).code());
        assertEquals(Code.NOT_FOUND, ShopEditService.saveUpgradePrices("CUSTOMTRAP", List.of()).code());
        assertEquals(Code.INVALID, ShopEditService.saveUpgradePrices("HASTE", List.of(new UpgradeTier(-1, "IRON"))).code());
        assertEquals(Code.INVALID, ShopEditService.saveUpgradePrices("HASTE", List.of(new UpgradeTier(1, "NONE"))).code());
        assertEquals(Code.INVALID, ShopEditService.saveUpgradePrices("HASTE", List.of(new UpgradeTier(1, null))).code());
        assertEquals(Code.INVALID, ShopEditService.saveUpgradePrices("HEALPOOL",
                List.of(new UpgradeTier(1, "IRON"), new UpgradeTier(2, "IRON"))).code());
    }

    @Test
    void guiPriceOverridesAreSavedKeptAndRemoved() {
        ShopGui current = ShopEditService.gui("shop").orElseThrow();
        assertTrue(current.upgradePrices().isEmpty());
        ShopGui withOverride = new ShopGui("shop", current.title(), 9, 0, 0, false, current.fill(), current.empty(),
                current.slots(), current.categories(), List.of(), List.of(),
                Map.of("haste", List.of(new UpgradeTier(1, "GOLD"))));
        assertEquals(Code.OK, ShopEditService.saveGui(withOverride).code());
        assertEquals(Map.of("HASTE", List.of(new UpgradeTier(1, "GOLD"))), ShopEditService.gui("shop").orElseThrow().upgradePrices());
        assertEquals(1, pricing.maxLevel(Upgrade.HASTE, guis.get("shop").getDefinition()));

        ShopGui keep = new ShopGui("shop", "Renamed", 9, 0, 0, false, current.fill(), current.empty(),
                current.slots(), current.categories(), List.of(), List.of());
        assertEquals(Code.OK, ShopEditService.saveGui(keep).code());
        assertEquals(1, ShopEditService.gui("shop").orElseThrow().upgradePrices().size());

        assertEquals(Code.INVALID, ShopEditService.saveGui(new ShopGui("shop", "R", 9, 0, 0, false, current.fill(),
                current.empty(), current.slots(), current.categories(), List.of(), List.of(),
                Map.of("NOPE", List.of()))).code());

        assertEquals(Code.OK, ShopEditService.saveGui(new ShopGui("shop", "R", 9, 0, 0, false, current.fill(),
                current.empty(), current.slots(), current.categories(), List.of(), List.of(), Map.of())).code());
        assertTrue(ShopEditService.gui("shop").orElseThrow().upgradePrices().isEmpty());
        assertFalse(guis.get("shop").getDefinition().extra().containsKey(UpgradePricing.GUI_KEY));
    }
}
