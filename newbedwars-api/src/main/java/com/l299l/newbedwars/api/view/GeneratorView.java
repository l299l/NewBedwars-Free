package com.l299l.newbedwars.api.view;

import org.bukkit.Location;

public interface GeneratorView {
    GeneratorKind getKind();

    Location getLocation();

    /**
     * The id of a {@link GeneratorKind#CUSTOM} generator in the {@code CustomGenerators} section of the
     * arena's generator file; null for the other kinds.
     *
     * @since 2.8-beta
     */
    default String getCustomId() {
        return null;
    }
}
