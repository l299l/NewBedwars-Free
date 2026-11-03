package com.l299l.newbedwars.npc;

import org.bukkit.Location;

public interface JoinNpc {

    String key();

    String displayName();

    JoinNpcAction action();

    Location location();

    double height();
}
