package com.l299l.newbedwars.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameVersionTest {

    @Test
    void parseKeepsTheLeadingNumbers() {
        assertArrayEquals(new int[]{26, 3}, GameVersion.parse("26.3.build.237-stable"));
        assertArrayEquals(new int[]{26, 1, 2}, GameVersion.parse("26.1.2.build.5-stable"));
        assertArrayEquals(new int[]{1, 17, 1}, GameVersion.parse("1.17.1-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[0], GameVersion.parse("build-1"));
        assertArrayEquals(new int[0], GameVersion.parse(null));
    }

    @Test
    void isAtLeastComparesMajorAndMinorWithBuildSuffixes() {
        assertTrue(GameVersion.isAtLeast("26.3.build.237-stable", 1, 20, 5));
        assertTrue(GameVersion.isAtLeast("26.3.build.237-stable", 1, 20, 3));
        assertTrue(GameVersion.isAtLeast("26.3.build.237-stable", 26, 3, 0));
        assertFalse(GameVersion.isAtLeast("26.3.build.237-stable", 26, 3, 1));
        assertFalse(GameVersion.isAtLeast("26.3.build.237-stable", 26, 4, 0));
        assertTrue(GameVersion.isAtLeast("26.1.2.build.5-stable", 26, 1, 2));
        assertFalse(GameVersion.isAtLeast("26.1.2.build.5-stable", 26, 1, 3));
        assertTrue(GameVersion.isAtLeast("26.1.2.build.5-stable", 1, 20, 5));
        assertFalse(GameVersion.isAtLeast("1.17.1-R0.1-SNAPSHOT", 1, 20, 3));
        assertFalse(GameVersion.isAtLeast("1.17.1-R0.1-SNAPSHOT", 1, 20, 5));
        assertTrue(GameVersion.isAtLeast("1.17.1-R0.1-SNAPSHOT", 1, 17, 1));
        assertTrue(GameVersion.isAtLeast("1.20.4-R0.1-SNAPSHOT", 1, 20, 3));
        assertFalse(GameVersion.isAtLeast("garbage", 1, 17, 0));
    }
}
