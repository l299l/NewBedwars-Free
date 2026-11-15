package com.l299l.newbedwars.arena.setup.auto;

import org.bukkit.Material;

public interface BlockSource {
    int minY();

    int highestY(int x, int z);

    Material type(int x, int y, int z);

    boolean isBedHead(int x, int y, int z);
}
