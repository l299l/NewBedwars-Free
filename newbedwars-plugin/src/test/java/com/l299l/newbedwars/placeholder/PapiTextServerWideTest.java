package com.l299l.newbedwars.placeholder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PapiTextServerWideTest {
    @AfterEach
    void tearDown() {
        PapiText.disable();
    }

    @Test
    void serverWidePlaceholdersResolveWithoutAPlayer() {
        PapiText.use((player, text) -> {
            assertNull(player);
            return text.replace("%online%", "&a5");
        });
        assertEquals("Shop \u00a7a5", PapiText.applyServerWide("Shop %online%"));
    }

    @Test
    void playerApplyStillSkipsWithoutAPlayer() {
        PapiText.use((player, text) -> "changed");
        assertEquals("Shop %online%", PapiText.apply(null, "Shop %online%"));
    }

    @Test
    void textStaysWithoutPlaceholderApi() {
        assertEquals("Shop %online%", PapiText.applyServerWide("Shop %online%"));
    }
}
