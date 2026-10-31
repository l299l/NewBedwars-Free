package com.l299l.newbedwars.arena;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockTrackerTest {

    @Test
    void placedBlocksAreTrackedPerBlock() {
        BlockTracker blocks = new BlockTracker();
        blocks.addPlaced(new Location(null, 1.2, 64.9, -3.5));
        assertTrue(blocks.isPlaced(new Location(null, 1, 64, -4)));
        assertFalse(blocks.isPlaced(new Location(null, 1, 65, -4)));
    }

    @Test
    void blastProtectionCanBeRemoved() {
        BlockTracker blocks = new BlockTracker();
        Location loc = new Location(null, 5, 70, 5);
        blocks.addBlastProtected(loc);
        assertTrue(blocks.isBlastProtected(loc));
        assertFalse(blocks.isPlaced(loc));
        blocks.removeBlastProtected(loc);
        assertFalse(blocks.isBlastProtected(loc));
    }

    @Test
    void clearForgetsBoth() {
        BlockTracker blocks = new BlockTracker();
        Location loc = new Location(null, 0, 0, 0);
        blocks.addPlaced(loc);
        blocks.addBlastProtected(loc);
        blocks.clear();
        assertFalse(blocks.isPlaced(loc));
        assertFalse(blocks.isBlastProtected(loc));
    }
}
