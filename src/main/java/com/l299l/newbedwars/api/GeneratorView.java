package com.l299l.newbedwars.api;

import org.bukkit.Location;

/** Read-only view of a resource generator. */
public interface GeneratorView {
    GeneratorKind getKind();

    Location getLocation();
}
