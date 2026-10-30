package com.l299l.newbedwars.text;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextAnimatorTest {

    @Test
    void noneOnlyTranslatesColors() {
        TextAnimator animator = new TextAnimator(TextEffect.NONE, "&aBedwars", null);
        assertEquals(ChatColor.GREEN + "Bedwars", animator.next("&aBedwars"));
        assertEquals(20, animator.period(1));
    }

    @Test
    void nullEffectIsNone() {
        assertEquals(TextEffect.NONE, new TextAnimator(null, "x", null).getEffect());
    }

    @Test
    void pulsingAlternatesBetweenPartnerColorAndOriginal() {
        TextAnimator animator = new TextAnimator(TextEffect.PULSING, "&aBedwars", null);
        assertEquals(ChatColor.DARK_GREEN + "Bedwars", animator.next("&aBedwars"));
        assertEquals(ChatColor.GREEN + "Bedwars", animator.next("&aBedwars"));
        assertEquals(ChatColor.DARK_GREEN + "Bedwars", animator.next("&aBedwars"));
    }

    @Test
    void pulsingWithoutLeadingColorUsesFallback() {
        TextAnimator animator = new TextAnimator(TextEffect.PULSING, "Bedwars", ChatColor.WHITE);
        assertEquals(ChatColor.GRAY + "Bedwars", animator.next("Bedwars"));
    }

    @Test
    void rainbowKeepsTheTextAndColorsOneCharacterPerFrame() {
        TextAnimator animator = new TextAnimator(TextEffect.RAINBOW, "&bAB", null);
        for (int frame = 0; frame < 6; frame++) {
            String out = animator.next("&bAB");
            assertEquals("AB", ChatColor.stripColor(out));
        }
    }

    @Test
    void goingChangingKeepsTheText() {
        TextAnimator animator = new TextAnimator(TextEffect.GOING_CHANGING, "&eBed", null);
        for (int frame = 0; frame < 5; frame++) {
            assertEquals("Bed", ChatColor.stripColor(animator.next("&eBed")));
        }
    }

    @Test
    void periods() {
        assertEquals(2, new TextAnimator(TextEffect.RAINBOW, "", null).period(2));
        assertEquals(1, new TextAnimator(TextEffect.GOING_CHANGING, "", null).period(1));
        assertEquals(15, new TextAnimator(TextEffect.PULSING, "", null).period(1));
        assertEquals(5, new TextAnimator(TextEffect.RAINBOW_PULSING, "", null).period(1));
    }

    @Test
    void randomColorIsNeverAFormat() {
        for (int i = 0; i < 200; i++) {
            assertTrue(TextAnimator.randomColor().isColor());
        }
    }
}
