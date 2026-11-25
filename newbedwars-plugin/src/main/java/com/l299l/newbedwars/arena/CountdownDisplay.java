package com.l299l.newbedwars.arena;

public record CountdownDisplay(boolean enabled, int lastSeconds) {
    public static final CountdownDisplay ALWAYS = new CountdownDisplay(true, 0);

    public boolean shows(int secondsLeft) {
        return enabled && (lastSeconds <= 0 || secondsLeft <= lastSeconds);
    }
}
