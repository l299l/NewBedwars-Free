package com.l299l.newbedwars.arena.generators;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratorHologramTextTest {

    private static final String TEXT = "&fDiamond Generator ; Tier &e /tier/ ; &fNext spawn in: &e /delay/s";

    @Test
    void keepsEveryLineWhenTheTimeIsShown() {
        assertEquals(TEXT, GeneratorHologramText.of(TEXT, true));
    }

    @Test
    void dropsTheCountdownLineWhenTheTimeIsHidden() {
        assertEquals("&fDiamond Generator ; Tier &e /tier/ ", GeneratorHologramText.of(TEXT, false));
    }

    @Test
    void keepsTheTextWhenOnlyTheCountdownIsLeft() {
        assertEquals("/delay/s", GeneratorHologramText.of("/delay/s", false));
    }
}
