package com.l299l.newbedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountdownDisplayTest {
    @Test
    void zeroLastSecondsShowsEverySecond() {
        assertTrue(new CountdownDisplay(true, 0).shows(20));
        assertTrue(new CountdownDisplay(true, 0).shows(1));
    }

    @Test
    void lastSecondsOnlyShowsTheEnd() {
        CountdownDisplay last3 = new CountdownDisplay(true, 3);
        assertFalse(last3.shows(4));
        assertTrue(last3.shows(3));
        assertTrue(last3.shows(1));
    }

    @Test
    void disabledNeverShows() {
        assertFalse(new CountdownDisplay(false, 0).shows(1));
        assertFalse(new CountdownDisplay(false, 5).shows(1));
    }
}
