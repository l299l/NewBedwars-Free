package com.l299l.newbedwars.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardFitTest {

    @Test
    void eightTeamsDefaultLayoutFitsFifteenAndKeepsLastLine() {
        List<String> lines = new ArrayList<>(List.of("date", " ", "Phase", "time", "  "));
        for (int i = 0; i < 8; i++) lines.add("team" + i);
        lines.addAll(List.of("   ", "Beds Broken", "    ", "server"));
        assertEquals(17, lines.size());
        List<String> fitted = ScoreboardManager.fitLines(lines);
        assertEquals(15, fitted.size());
        assertEquals("server", fitted.get(14));
        assertTrue(fitted.contains("Beds Broken"));
        assertFalse(fitted.contains("    "));
    }

    @Test
    void nonBlankLinesAreCutFromTheEnd() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 20; i++) lines.add("line" + i);
        assertEquals(lines.subList(0, 15), ScoreboardManager.fitLines(lines));
    }

    @Test
    void shortListUnchanged() {
        List<String> lines = List.of("a", " ", "b");
        assertEquals(lines, ScoreboardManager.fitLines(lines));
    }

    @Test
    void truncateDoesNotEndOnLoneSectionSign() {
        String s = "a".repeat(39) + "§c" + "tail";
        assertEquals("a".repeat(39), ScoreboardManager.truncateEntry(s, 40));
        assertEquals("abc", ScoreboardManager.truncateEntry("abc", 40));
    }

    @Test
    void truncatedEntriesStayUniqueAndWithinLimit() {
        String base = "x".repeat(50);
        List<String> out = ScoreboardManager.makeUnique(List.of(base + "1", base + "2", base + "3"), 40);
        assertEquals(3, new HashSet<>(out).size());
        for (String o : out) assertTrue(o.length() <= 40);
    }
}
