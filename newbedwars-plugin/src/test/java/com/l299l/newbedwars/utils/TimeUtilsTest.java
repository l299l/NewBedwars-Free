package com.l299l.newbedwars.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilsTest {
    @Test
    void defaultFormatIsUnchanged() {
        assertEquals("45s", TimeUtils.formatTime(45, false));
        assertEquals("5m 0s", TimeUtils.formatTime(300, false));
        assertEquals("1m 5s", TimeUtils.formatTime(65, false));
        assertEquals("0m 0s", TimeUtils.formatTime(-3, false));
        assertEquals("45s", TimeUtils.formatTime(45));
    }

    @Test
    void clockFormat() {
        assertEquals("05:00", TimeUtils.formatTime(300, true));
        assertEquals("00:45", TimeUtils.formatTime(45, true));
        assertEquals("00:00", TimeUtils.formatTime(-1, true));
        assertEquals("65:01", TimeUtils.formatTime(3901, true));
    }
}
