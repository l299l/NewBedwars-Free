package com.l299l.newbedwars.arena.setup.auto;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChunkAnalyzerTest {

    private static final class Fake implements BlockSource {
        final Map<Pos, Material> blocks = new HashMap<>();
        final Set<Pos> heads = new HashSet<>();

        @Override
        public int minY() {
            return 0;
        }

        @Override
        public int highestY(int x, int z) {
            int top = -1;
            for (Pos pos : blocks.keySet()) {
                if (pos.x() == x && pos.z() == z) top = Math.max(top, pos.y());
            }
            return top;
        }

        @Override
        public Material type(int x, int y, int z) {
            return blocks.getOrDefault(new Pos(x, y, z), Material.AIR);
        }

        @Override
        public boolean isBedHead(int x, int y, int z) {
            return heads.contains(new Pos(x, y, z));
        }
    }

    @Test
    void findsBedHeadsGeneratorBlocksAndLowestSolid() {
        Fake source = new Fake();
        source.blocks.put(new Pos(1, 40, 1), Material.STONE);
        source.blocks.put(new Pos(2, 50, 2), Material.RED_BED);
        source.heads.add(new Pos(2, 50, 2));
        source.blocks.put(new Pos(2, 50, 3), Material.RED_BED);
        source.blocks.put(new Pos(4, 50, 4), Material.DIAMOND_BLOCK);
        source.blocks.put(new Pos(5, 50, 5), Material.EMERALD_BLOCK);
        source.blocks.put(new Pos(6, 50, 6), Material.IRON_BLOCK);
        source.blocks.put(new Pos(7, 50, 7), Material.GOLD_BLOCK);
        ScanData data = ChunkAnalyzer.analyze(source, 2, -1);
        assertEquals(1, data.beds().size());
        assertEquals(new Pos(34, 50, -14), data.beds().get(0).pos());
        assertEquals(ChatColor.RED, data.beds().get(0).color());
        assertEquals(4, data.blocks().size());
        assertEquals(40, data.lowestSolidY());
    }

    @Test
    void emptyChunkHasNothing() {
        ScanData data = ChunkAnalyzer.analyze(new Fake(), 0, 0);
        assertTrue(data.beds().isEmpty());
        assertNull(data.lowestSolidY());
    }
}
