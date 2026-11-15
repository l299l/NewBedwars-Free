package com.l299l.newbedwars.arena.setup.auto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignMarkTest {
    private static final Pos POS = new Pos(1, 2, 3);

    @Test
    void parsesKindTeamAndExtra() {
        SignMark mark = SignMark.parse(POS, new String[]{"[Shop]", " Red ", "creeper", ""});
        assertEquals(SignMark.Kind.SHOP, mark.kind());
        assertEquals("Red", mark.team());
        assertEquals("creeper", mark.extra());
        assertEquals(POS, mark.pos());
    }

    @Test
    void acceptsAliasesAndMissingLines() {
        assertEquals(SignMark.Kind.UPGRADES, SignMark.parse(POS, new String[]{"[upgrade]"}).kind());
        assertEquals(SignMark.Kind.GENERATOR, SignMark.parse(POS, new String[]{"[GENERATOR]", null, null, null}).kind());
        assertEquals("", SignMark.parse(POS, new String[]{"[spawn]"}).team());
    }

    @Test
    void ignoresOtherSigns() {
        assertNull(SignMark.parse(POS, new String[]{"hello", "", "", ""}));
        assertNull(SignMark.parse(POS, new String[]{}));
        assertNull(SignMark.parse(POS, null));
    }
}
