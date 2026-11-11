package com.l299l.newbedwars.config.data.yaml.guis;

import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.GUISave;
import com.l299l.newbedwars.gui.model.GUICategory;
import com.l299l.newbedwars.gui.model.GUIUpgrade;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GuiResolverTest {
    private final Map<String, CustomItem> items = new HashMap<>();
    private final GuiResolver resolver = new GuiResolver(name -> items.get(name.toLowerCase()));

    private CustomItem item(String name) {
        CustomItem item = mock(CustomItem.class);
        items.put(name.toLowerCase(), item);
        return item;
    }

    @Test
    void resolvesSlotsCategoriesFillAndUpgrades() {
        CustomItem wool = item("Wool");
        CustomItem fill = item("Fill");
        item("Forge");
        List<String> slots = new ArrayList<>(Collections.nCopies(9, "empty"));
        slots.set(0, "cat:blocks");
        slots.set(1, "fill");
        slots.set(2, "ci:wool");
        slots.set(3, "upg:Forge");
        GuiDefinition d = new GuiDefinition("g", "ShopGUI", "G", 9, true, "ci:Fill", "empty", 0, 0, slots,
                List.of(new GuiDefinition.Category("blocks", "Blocks", "Wool", "", List.of("Wool", "upg:Forge"))), Map.of());
        GUISave save = resolver.resolve(d);
        assertTrue(save.getProblems().isEmpty(), save.getProblems().toString());
        assertInstanceOf(GUICategory.class, save.getItem(0));
        assertSame(fill, save.getItem(1));
        assertSame(wool, save.getItem(2));
        assertInstanceOf(GUIUpgrade.class, save.getItem(3));
        assertNull(save.getItem(4));
        assertEquals(2, save.getCategories().get("blocks").items().size());
        assertSame(d, save.getDefinition());
    }

    @Test
    void brokenReferencesAreProblemsNotErrors() {
        item("Wool");
        List<String> slots = new ArrayList<>(Collections.nCopies(9, "empty"));
        slots.set(0, "cat:missing");
        slots.set(1, "ci:Nope");
        slots.set(2, "upg:NoIcon");
        slots.set(3, "rubbish");
        GuiDefinition d = new GuiDefinition("g", "ShopGUI", "G", 9, true, "ci:Gone", "empty", 0, 0, slots,
                List.of(new GuiDefinition.Category("a", "A", "Missing", "", List.of()),
                        new GuiDefinition.Category("b", "B", "Wool", "", List.of("Nope", "Wool"))), Map.of());
        GUISave save = resolver.resolve(d);
        assertEquals(7, save.getProblems().size(), save.getProblems().toString());
        assertNull(save.getItem(0));
        assertNull(save.getItem(1));
        assertFalse(save.getCategories().containsKey("a"));
        assertEquals(1, save.getCategories().get("b").items().size());
    }

    @Test
    void headerAndFooterAreClamped() {
        GuiDefinition d = new GuiDefinition("g", "ShopGUI", "G", 9, true, "empty", "empty", 18, 9,
                Collections.nCopies(9, "empty"), List.of(), Map.of());
        GUISave save = resolver.resolve(d);
        assertEquals(9, save.getHeaderSize());
        assertEquals(0, save.getFooterSize());
    }
}
