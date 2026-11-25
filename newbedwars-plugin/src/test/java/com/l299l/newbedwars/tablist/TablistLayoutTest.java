package com.l299l.newbedwars.tablist;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TablistLayoutTest {
    private static TablistSection section(String header, String footer, String format) {
        return new TablistSection(header == null ? null : List.of(header), footer == null ? null : List.of(footer), format);
    }

    @Test
    void mergeKeepsOwnValuesAndFillsGapsFromFallback() {
        TablistSection merged = section("h", null, null).merge(section("fh", "ff", "fmt"));
        assertEquals(List.of("h"), merged.header());
        assertEquals(List.of("ff"), merged.footer());
        assertEquals("fmt", merged.playerFormat());
    }

    @Test
    void mergeWithNullFallbackReturnsSameSection() {
        TablistSection own = section("h", "f", "p");
        assertSame(own, own.merge(null));
    }

    @Test
    void emptyMergedWithEmptyStaysEmpty() {
        TablistSection merged = TablistSection.EMPTY.merge(TablistSection.EMPTY);
        assertNull(merged.header());
        assertNull(merged.footer());
        assertNull(merged.playerFormat());
    }

    @Test
    void waitingFallsBackToPlaying() {
        TablistLayout layout = new TablistLayout("L", section(null, "wf", null), section("ph", "pf", "pp"), TablistSection.EMPTY);
        TablistSection s = layout.resolve(TablistLayout.State.WAITING);
        assertEquals(List.of("ph"), s.header());
        assertEquals(List.of("wf"), s.footer());
        assertEquals("pp", s.playerFormat());
    }

    @Test
    void playingFallsBackToWaiting() {
        TablistLayout layout = new TablistLayout("L", section("wh", "wf", "wp"), section(null, null, "pp"), TablistSection.EMPTY);
        TablistSection s = layout.resolve(TablistLayout.State.PLAYING);
        assertEquals(List.of("wh"), s.header());
        assertEquals(List.of("wf"), s.footer());
        assertEquals("pp", s.playerFormat());
    }

    @Test
    void spectatorFallsBackToPlayingThenWaiting() {
        TablistLayout layout = new TablistLayout("L", section("wh", "wf", "wp"), section("ph", null, "pp"), section(null, null, "sp"));
        TablistSection s = layout.resolve(TablistLayout.State.SPECTATOR);
        assertEquals(List.of("ph"), s.header());
        assertEquals(List.of("wf"), s.footer());
        assertEquals("sp", s.playerFormat());
    }

    @Test
    void ownSectionAlwaysWins() {
        TablistLayout layout = new TablistLayout("L", section("wh", "wf", "wp"), section("ph", "pf", "pp"), section("sh", "sf", "sp"));
        assertEquals(List.of("wh"), layout.resolve(TablistLayout.State.WAITING).header());
        assertEquals(List.of("ph"), layout.resolve(TablistLayout.State.PLAYING).header());
        assertEquals(List.of("sh"), layout.resolve(TablistLayout.State.SPECTATOR).header());
        assertEquals("L", layout.id());
    }
}
