package com.l299l.newbedwars.arena.shops.customitems;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.shop.ItemPrice;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.data.yaml.guis.GuiDefinition;
import com.l299l.newbedwars.gui.GUIManager;
import com.l299l.newbedwars.gui.GUISave;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemPricingTest {
    private Logger logger;
    private GUIManager guiManager;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        logger = mock(Logger.class);
        guiManager = mock(GUIManager.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getGuiManager()).thenReturn(guiManager);
        NewBedwars.plugin = plugin;
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    private static String unique() {
        return "gui" + UUID.randomUUID();
    }

    private static GuiDefinition gui(String id, Map<String, Object> prices) {
        Map<String, Object> extra = new LinkedHashMap<>();
        if (prices != null) extra.put("ItemPrices", prices);
        return new GuiDefinition(id, "ShopGUI", id, 54, "fill", "empty", 0, 0, List.of(), List.of(), extra);
    }

    private static CustomItem item(String name, int price, PriceType type, Material other, int amount) {
        CustomItem item = mock(CustomItem.class);
        when(item.getName()).thenReturn(name);
        when(item.getPrice()).thenReturn(price);
        when(item.getPriceType()).thenReturn(type);
        when(item.getOtherPriceMaterial()).thenReturn(other);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }

    private static Map<String, Object> entry(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private IArena arenaWithShop(GuiDefinition shop) {
        IArena arena = mock(IArena.class);
        when(arena.getShopGuiId()).thenReturn(shop.id());
        GUISave save = mock(GUISave.class);
        when(save.getDefinition()).thenReturn(shop);
        when(guiManager.getGui(shop.id())).thenReturn(save);
        return arena;
    }

    @Test
    void parseReadsPriceCurrencyAndAmount() {
        ItemPrice p = ItemPricing.parse(entry("price", 8, "currency", "gold", "amount", 16));
        assertEquals(new ItemPrice(8, "GOLD", 16), p);
    }

    @Test
    void parseDefaultsCurrencyAndAmount() {
        ItemPrice p = ItemPricing.parse(entry("price", 0));
        assertEquals(new ItemPrice(0, null, 0), p);
    }

    @Test
    void parseRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse("12"));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("currency", "GOLD")));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", 2.5)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", "3")));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", -1)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", ItemPricing.MAX_PRICE + 1)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", 1, "amount", 0)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", 1, "amount", 65)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", 1, "currency", "COPPER")));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", 1, "currency", "OTHER")));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.parse(entry("price", 1, "cost", 3)));
    }

    @Test
    void parseAcceptsBoundaries() {
        assertEquals(ItemPricing.MAX_PRICE, ItemPricing.parse(entry("price", ItemPricing.MAX_PRICE, "amount", 64)).price());
        assertEquals(1, ItemPricing.parse(entry("price", 0, "amount", 1)).amount());
    }

    @Test
    void applyWithoutCurrencyKeepsItemCurrencyIncludingOther() {
        ItemPricing.Offer base = new ItemPricing.Offer(4, PriceType.OTHER, Material.DIAMOND_BLOCK, 3);
        ItemPricing.Offer o = ItemPricing.apply(base, new ItemPrice(9, null, 0));
        assertEquals(9, o.price());
        assertEquals(PriceType.OTHER, o.priceType());
        assertEquals(Material.DIAMOND_BLOCK, o.otherMaterial());
        assertEquals(3, o.amount());
    }

    @Test
    void applyWithCurrencyClearsOtherMaterial() {
        ItemPricing.Offer base = new ItemPricing.Offer(4, PriceType.OTHER, Material.DIAMOND_BLOCK, 3);
        ItemPricing.Offer o = ItemPricing.apply(base, new ItemPrice(2, "emerald", 8));
        assertEquals(PriceType.EMERALD, o.priceType());
        assertNull(o.otherMaterial());
        assertEquals(8, o.amount());
        assertEquals(Material.EMERALD, o.priceMaterial());
    }

    @Test
    void offerPriceLabelAndMaterial() {
        assertEquals("DIAMOND_BLOCK", new ItemPricing.Offer(1, PriceType.OTHER, Material.DIAMOND_BLOCK, 1).priceLabel());
        assertEquals("OTHER", new ItemPricing.Offer(1, PriceType.OTHER, null, 1).priceLabel());
        assertEquals("IRON", new ItemPricing.Offer(1, PriceType.IRON, null, 1).priceLabel());
        assertEquals(Material.GOLD_INGOT, new ItemPricing.Offer(1, PriceType.GOLD, null, 1).priceMaterial());
        assertNull(new ItemPricing.Offer(1, PriceType.NONE, null, 1).priceMaterial());
    }

    @Test
    void baseCopiesTheItem() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        assertEquals(new ItemPricing.Offer(4, PriceType.IRON, null, 16), ItemPricing.base(wool));
    }

    @Test
    void shownGuiOverrideWins() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        GuiDefinition shown = gui(unique(), Map.of("Wool", entry("price", 2, "currency", "GOLD")));
        IArena arena = arenaWithShop(gui(unique(), Map.of("Wool", entry("price", 99))));
        ItemPricing.Offer o = ItemPricing.offer(wool, shown, arena);
        assertEquals(2, o.price());
        assertEquals(PriceType.GOLD, o.priceType());
        assertEquals(16, o.amount());
    }

    @Test
    void fallsBackToTheArenaShopGui() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        GuiDefinition shown = gui(unique(), Map.of("Other", entry("price", 1)));
        IArena arena = arenaWithShop(gui(unique(), Map.of("Wool", entry("price", 7, "amount", 32))));
        ItemPricing.Offer o = ItemPricing.offer(wool, shown, arena);
        assertEquals(7, o.price());
        assertEquals(32, o.amount());
        assertEquals(PriceType.IRON, o.priceType());
    }

    @Test
    void nullShownGuiUsesTheArenaShop() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        IArena arena = arenaWithShop(gui(unique(), Map.of("Wool", entry("price", 5))));
        assertEquals(5, ItemPricing.offer(wool, null, arena).price());
    }

    @Test
    void baseWhenNothingOverrides() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        assertEquals(ItemPricing.base(wool), ItemPricing.offer(wool, gui(unique(), null), null));
        assertEquals(ItemPricing.base(wool), ItemPricing.offer(wool, null, null));
        IArena noShop = mock(IArena.class);
        assertEquals(ItemPricing.base(wool), ItemPricing.offer(wool, null, noShop));
        IArena missingGui = mock(IArena.class);
        when(missingGui.getShopGuiId()).thenReturn("nope");
        assertEquals(ItemPricing.base(wool), ItemPricing.offer(wool, null, missingGui));
    }

    @Test
    void sameShownAndShopGuiIsNotReadTwice() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        GuiDefinition shop = gui(unique(), Map.of("Other", entry("price", 1)));
        IArena arena = arenaWithShop(shop);
        assertEquals(ItemPricing.base(wool), ItemPricing.offer(wool, shop, arena));
    }

    @Test
    void itemKeyIsCaseInsensitive() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        GuiDefinition shown = gui(unique(), Map.of("wOOL", entry("price", 3)));
        assertEquals(3, ItemPricing.offer(wool, shown, null).price());
    }

    @Test
    void invalidEntryUsesBasePriceAndWarnsOnce() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        GuiDefinition shown = gui(unique(), Map.of("Wool", entry("price", 2.5)));
        for (int i = 0; i < 3; i++) {
            assertEquals(ItemPricing.base(wool), ItemPricing.offer(wool, shown, null));
        }
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(logger, org.mockito.Mockito.times(1)).warning(msg.capture());
        assertTrue(msg.getValue().contains("Wool"));
        assertTrue(msg.getValue().contains(shown.id()));
    }

    @Test
    void invalidShownEntryFallsBackToTheShop() {
        CustomItem wool = item("Wool", 4, PriceType.IRON, null, 16);
        GuiDefinition shown = gui(unique(), Map.of("Wool", entry("price", "x")));
        IArena arena = arenaWithShop(gui(unique(), Map.of("Wool", entry("price", 7))));
        assertEquals(7, ItemPricing.offer(wool, shown, arena).price());
    }

    @Test
    void overridesSkipsInvalidEntriesAndKeepsValidOnes() {
        GuiDefinition g = gui(unique(), Map.of("A", entry("price", 1), "B", entry("price", -5)));
        Map<String, ItemPrice> o = ItemPricing.overrides(g);
        assertEquals(1, o.size());
        assertEquals(1, o.get("A").price());
        assertTrue(ItemPricing.overrides(null).isEmpty());
        assertTrue(ItemPricing.overrides(gui(unique(), null)).isEmpty());
    }

    @Test
    void serializeWritesOnlySetFields() {
        assertEquals(entry("price", 5), ItemPricing.serialize(new ItemPrice(5, null, 0)));
        assertEquals(entry("price", 5, "currency", "GOLD", "amount", 2), ItemPricing.serialize(new ItemPrice(5, "gold", 2)));
    }

    @Test
    void serializeRoundTripsThroughParse() {
        ItemPrice p = new ItemPrice(12, "EMERALD", 4);
        assertEquals(p, ItemPricing.parse(ItemPricing.serialize(p)));
    }

    @Test
    void validateNormalisesAndRejects() {
        assertEquals(new ItemPrice(3, "GOLD", 0), ItemPricing.validate(new ItemPrice(3, "gold", 0)));
        assertEquals(new ItemPrice(3, null, 5), ItemPricing.validate(new ItemPrice(3, " ", 5)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.validate(null));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.validate(new ItemPrice(-1, null, 0)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.validate(new ItemPrice(ItemPricing.MAX_PRICE + 1, null, 0)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.validate(new ItemPrice(1, null, 65)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.validate(new ItemPrice(1, null, -1)));
        assertThrows(IllegalArgumentException.class, () -> ItemPricing.validate(new ItemPrice(1, "COPPER", 0)));
    }
}
