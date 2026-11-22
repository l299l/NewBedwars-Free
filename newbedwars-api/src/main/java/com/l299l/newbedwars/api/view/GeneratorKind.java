package com.l299l.newbedwars.api.view;

public enum GeneratorKind {
    /** The iron and gold generator of a team base. */
    BASIC,
    DIAMOND,
    EMERALD,
    /**
     * A custom generator from the {@code CustomGenerators} section of a generator file; see
     * {@link GeneratorView#getCustomId()}.
     *
     * @since 2.8-beta
     */
    CUSTOM
}
