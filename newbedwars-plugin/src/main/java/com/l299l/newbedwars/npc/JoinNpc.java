package com.l299l.newbedwars.npc;

import org.bukkit.Location;

/** A clickable NPC attached to NewBedwars (Citizens or fallback). */
public interface JoinNpc {
    /** Unique key, e.g. {@code citizens:5} or {@code fallback:3}. */
    String key();

    /** Name shown in admin messages. */
    String displayName();

    JoinNpcAction action();

    /** Where the NPC stands now, or null when it is not spawned. */
    Location location();

    /** Height of the NPC entity (the hologram is placed above it). */
    double height();
}
