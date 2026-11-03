package com.l299l.newbedwars.text;

public enum TextEffect {
    NONE,
    RAINBOW,
    GOING_CHANGING,
    PULSING,
    RAINBOW_PULSING;

    public boolean isScrolling() {
        return this == RAINBOW || this == GOING_CHANGING;
    }
}
