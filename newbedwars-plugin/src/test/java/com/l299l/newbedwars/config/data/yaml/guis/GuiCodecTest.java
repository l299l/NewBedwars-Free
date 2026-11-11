package com.l299l.newbedwars.config.data.yaml.guis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GuiCodecTest {

    private static String resource(String name) throws IOException {
        try (InputStream in = GuiCodecTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void bundledGuisSurviveARoundTrip() throws IOException {
        for (String name : List.of("exampleShop.yml", "exampleUpgrade.yml")) {
            GuiDefinition parsed = GuiCodec.parse(resource(name));
            assertEquals(parsed, GuiCodec.parse(GuiCodec.toYaml(parsed)), name);
        }
    }

    @Test
    void readsTheExampleShop() throws IOException {
        GuiDefinition d = GuiCodec.parse(resource("exampleShop.yml"));
        assertEquals("exampleShop", d.id());
        assertEquals(54, d.slots().size());
        assertEquals("category:home", d.slots().get(0));
        assertEquals("fill", d.slots().get(9));
        assertEquals("ci:fill", d.fill());
        assertEquals("empty", d.empty());
        assertEquals(18, d.headerSize());
        assertEquals("&eHome", d.category("home").name());
        assertEquals("&7Browse building blocks", d.category("blocks").description());
        assertTrue(d.category("home").items().isEmpty());
    }

    @Test
    void quotesColourCodesQuotesAndBackslashes() {
        List<String> slots = new ArrayList<>(Collections.nCopies(9, "empty"));
        slots.set(0, "ci:Wool");
        GuiDefinition d = new GuiDefinition("t", "ShopGUI", "&a\"Shop\" \\ ünïcode: #1", 9, false, "empty", "empty", 0, 0,
                slots, List.of(new GuiDefinition.Category("home", "&eHome", "Wool", "a; b", List.of("Wool", "upg:Forge"))), Map.of());
        GuiDefinition back = GuiCodec.parse(GuiCodec.toYaml(d));
        assertEquals(d, back);
    }

    @Test
    void keepsUnknownTopLevelKeys() {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("Custom", Map.of("a", 1));
        GuiDefinition d = new GuiDefinition("t", "ShopGUI", "T", 9, true, "empty", "empty", 0, 0,
                Collections.nCopies(9, "empty"), List.of(), extra);
        GuiDefinition back = GuiCodec.parse(GuiCodec.toYaml(d));
        assertEquals(Map.of("a", 1), back.extra().get("Custom"));
    }

    @Test
    void missingSlotsFillAndEmptyBecomeEmpty() {
        GuiDefinition d = GuiCodec.parse("GuiId: x\nSize: 18\nSlots:\n  2: ci:Wool\n");
        assertEquals(18, d.slots().size());
        assertEquals("empty", d.slots().get(0));
        assertEquals("ci:Wool", d.slots().get(1));
        assertEquals("empty", d.fill());
        assertEquals("ShopGUI", d.guiClass());
        assertEquals("x", d.title());
    }

    @Test
    void refusesBadFiles() {
        assertThrows(IllegalArgumentException.class, () -> GuiCodec.parse("Size: 9"));
        assertThrows(IllegalArgumentException.class, () -> GuiCodec.parse("GuiId: x\nSize: 10"));
        assertThrows(IllegalArgumentException.class, () -> GuiCodec.parse("- a\n- b"));
    }

    @Test
    void refsParseAllSpellings() {
        assertEquals(GuiRefs.Kind.ITEM, GuiRefs.slot("customItem:Wool").kind());
        assertEquals(GuiRefs.Kind.CATEGORY, GuiRefs.slot("cat:home").kind());
        assertEquals(GuiRefs.Kind.UPGRADE, GuiRefs.slot("upgrade:Forge").kind());
        assertEquals(GuiRefs.Kind.FILL, GuiRefs.slot("FILL").kind());
        assertEquals(GuiRefs.Kind.EMPTY, GuiRefs.slot("empty").kind());
        assertEquals(GuiRefs.Kind.INVALID, GuiRefs.slot("Wool").kind());
        assertEquals(GuiRefs.Kind.INVALID, GuiRefs.slot("ci:").kind());
        assertEquals(GuiRefs.Kind.ITEM, GuiRefs.categoryItem("Wool").kind());
        assertEquals(GuiRefs.Kind.INVALID, GuiRefs.fillOrEmpty("cat:home").kind());
        assertTrue(GuiRefs.refersToItem("ci:wool", "Wool", false));
        assertTrue(GuiRefs.refersToItem("upg:Forge", "forge", true));
        assertFalse(GuiRefs.refersToItem("cat:wool", "Wool", false));
    }
}
