package com.l299l.newbedwars.api.mode;

import java.util.List;

/**
 * Result of editing modes, phase lists or generator configs through {@link ModeService}.
 *
 * @param code     what happened
 * @param problems with {@link Code#INVALID}: short human-readable reasons (English); with {@link Code#FAILED}: the error
 * @param usedBy   with {@link Code#IN_USE}: who still uses it, e.g. {@code mode Squads} or {@code arena castle}
 * @since 2.8-beta
 */
public record EditResult(Code code, List<String> problems, List<String> usedBy) {
    public EditResult {
        problems = problems == null ? List.of() : List.copyOf(problems);
        usedBy = usedBy == null ? List.of() : List.copyOf(usedBy);
    }

    public enum Code {
        /** The change was written to disk and loaded. */
        OK,
        /** No mode, phase list or generator config with that id; nothing was changed. */
        NOT_FOUND,
        /** The submitted data is not valid (see {@link EditResult#problems()}); nothing was changed. */
        INVALID,
        /** A mode or arena still uses it (see {@link EditResult#usedBy()}); nothing was changed. */
        IN_USE,
        /** It ships with NewBedwars and can be edited but not deleted; nothing was changed. */
        BUILT_IN,
        /** The file could not be written or read; see {@link EditResult#problems()}. */
        FAILED
    }
}
