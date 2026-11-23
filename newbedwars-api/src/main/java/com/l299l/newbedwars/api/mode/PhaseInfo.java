package com.l299l.newbedwars.api.mode;

import java.util.List;

/**
 * One phase of a phase list. Its actions run when the phase starts; the next phase starts {@code duration} seconds later.
 *
 * @param name     display name
 * @param duration seconds until the next phase, 0 for the last phase
 * @param actions  actions as written in {@code modes.yml}, e.g. {@code DIAMOND_GENERATOR:2}
 * @since 2.8-beta
 */
public record PhaseInfo(String name, int duration, List<String> actions) {
    public PhaseInfo {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
