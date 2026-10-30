package com.l299l.newbedwars.arena;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

/** Blocks placed by players during a game, and the placed blocks that are blast-protected. */
public class BlockTracker {
    private final Set<String> placed = new HashSet<>();
    private final Set<String> blastProtected = new HashSet<>();

    private static String key(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public void addPlaced(Location loc) {
        placed.add(key(loc));
    }

    public boolean isPlaced(Location loc) {
        return placed.contains(key(loc));
    }

    public void addBlastProtected(Location loc) {
        blastProtected.add(key(loc));
    }

    public boolean isBlastProtected(Location loc) {
        return blastProtected.contains(key(loc));
    }

    public void removeBlastProtected(Location loc) {
        blastProtected.remove(key(loc));
    }

    public void clear() {
        placed.clear();
        blastProtected.clear();
    }
}
