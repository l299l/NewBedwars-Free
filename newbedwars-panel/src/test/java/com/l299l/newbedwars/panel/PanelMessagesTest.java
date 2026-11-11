package com.l299l.newbedwars.panel;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PanelMessagesTest {
    @Test
    void picksLanguageAndFallsBack() {
        PanelMessages m = new PanelMessages("en");
        m.put("en", Map.of("Hello", "Hi {0}, {1}", "Only", "english"));
        m.put("pl", Map.of("Hello", "Cześć {0}, {1}", "Only", ""));
        assertEquals("pl", m.language("pl_PL"));
        assertEquals("pl", m.language("PL-pl"));
        assertEquals("en", m.language("de_de"));
        assertEquals("en", m.language(null));
        assertEquals("Cześć Leon, 2", m.get("pl", "Hello", "Leon", 2));
        assertEquals("english", m.get("pl", "Only"));
        assertEquals("Missing", m.get("pl", "Missing"));
    }

    @Test
    void defaultLanguageIsUsedForConsole() {
        PanelMessages m = new PanelMessages("PL");
        m.put("en", Map.of("A", "a"));
        m.put("pl", Map.of("A", "ą"));
        assertEquals("pl", m.language(null));
        assertEquals("ą", m.get(m.language("fr_fr"), "A"));
    }
}
