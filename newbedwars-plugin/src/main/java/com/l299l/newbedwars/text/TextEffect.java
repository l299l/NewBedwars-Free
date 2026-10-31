package com.l299l.newbedwars.text;

/** Title animation of a boss bar or scoreboard ({@code Effects} / {@code TitleEffect} in the YAML). */
public enum TextEffect {
    NONE,
    RAINBOW,
    GOING_CHANGING,
    PULSING,
    RAINBOW_PULSING;

    /** Effects that move along the text one character per frame. */
    public boolean isScrolling() {
        return this == RAINBOW || this == GOING_CHANGING;
    }
}
