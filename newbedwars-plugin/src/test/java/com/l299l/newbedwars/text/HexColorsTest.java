package com.l299l.newbedwars.text;

import com.l299l.newbedwars.placeholder.PapiText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HexColorsTest {
    @Test
    void hexCodesBecomeMinecraftColors() {
        assertEquals("§x§E§E§a§a§0§0Hi §x§0§0§0§0§0§0!", HexColors.translate("&#EEaa00Hi &#000000!"));
    }

    @Test
    void otherTextIsUnchanged() {
        assertEquals("#EEEEEE &#12345 &#GGGGGG &a", HexColors.translate("#EEEEEE &#12345 &#GGGGGG &a"));
        assertNull(HexColors.translate(null));
    }

    @Test
    void textShownThroughPlaceholderHandlingGetsHexColorsToo() {
        assertEquals("§x§f§f§0§0§0§0Red", PapiText.apply(null, "&#ff0000Red"));
    }
}
