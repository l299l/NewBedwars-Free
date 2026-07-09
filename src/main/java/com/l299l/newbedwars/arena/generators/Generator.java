package com.l299l.newbedwars.arena.generators;

import org.bukkit.Location;

public interface Generator {
    void start();
    void stop();
    GeneratorType getType();
    Location getLocation();
    boolean upgrade();
    boolean upgrade(Integer level);
}
