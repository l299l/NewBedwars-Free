package com.l299l.newbedwars.api.mode;

import java.util.List;

/**
 * Result of {@link ModeService#applyMode}.
 *
 * @param code        what happened
 * @param missing     with {@link Code#MISSING_PARTS}: generator configs, phase lists or GUIs the mode names that don't exist
 * @param teamsDiffer with {@link Code#OK}: the arena's team count differs from the mode's {@link ModeInfo#teams()}
 * @since 2.8-beta
 */
public record ModeResult(Code code, List<String> missing, boolean teamsDiffer) {
    public ModeResult {
        missing = missing == null ? List.of() : List.copyOf(missing);
    }

    public enum Code {
        /** The mode was copied into the arena and the arena was saved. */
        OK,
        /** No arena or no mode with that name. */
        NOT_FOUND,
        /** The mode names a part that doesn't exist; nothing was changed. */
        MISSING_PARTS,
        /** The arena is not waiting for players (a game is starting or running); nothing was changed. */
        BUSY
    }
}
