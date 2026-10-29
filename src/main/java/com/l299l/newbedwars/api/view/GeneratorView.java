package com.l299l.newbedwars.api.view;

import org.bukkit.Location;

/** Read-only view of a resource generator. */
public interface GeneratorView {
    GeneratorKind getKind();

    Location getLocation();
}
