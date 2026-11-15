package com.l299l.newbedwars.arena.setup.auto;

import com.l299l.newbedwars.utils.TeamColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

public final class ChunkAnalyzer {
    private static final Map<Material, ChatColor> BEDS = new EnumMap<>(Material.class);

    static {
        for (Material material : Material.values()) {
            if (TeamColor.kindOf(material) != TeamColor.Kind.BED) continue;
            ChatColor color = TeamColor.fromMaterial(material);
            if (color != null) BEDS.put(material, color);
        }
    }

    private ChunkAnalyzer() {}

    public static ScanData analyze(BlockSource source, int chunkX, int chunkZ) {
        ScanData data = new ScanData();
        int minY = source.minY();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = source.highestY(x, z);
                boolean solidFound = false;
                for (int y = minY; y <= top; y++) {
                    Material type = source.type(x, y, z);
                    if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) continue;
                    if (!solidFound) {
                        data.solidAt(y);
                        solidFound = true;
                    }
                    classify(source, data, type, chunkX * 16 + x, y, chunkZ * 16 + z, x, z);
                }
            }
        }
        return data;
    }

    private static void classify(BlockSource source, ScanData data, Material type, int wx, int y, int wz, int lx, int lz) {
        switch (type) {
            case DIAMOND_BLOCK -> data.blocks().add(new ScanData.GenBlock(new Pos(wx, y, wz), ScanData.Block.DIAMOND));
            case EMERALD_BLOCK -> data.blocks().add(new ScanData.GenBlock(new Pos(wx, y, wz), ScanData.Block.EMERALD));
            case IRON_BLOCK -> data.blocks().add(new ScanData.GenBlock(new Pos(wx, y, wz), ScanData.Block.IRON));
            case GOLD_BLOCK -> data.blocks().add(new ScanData.GenBlock(new Pos(wx, y, wz), ScanData.Block.GOLD));
            default -> {
                ChatColor color = BEDS.get(type);
                if (color != null && source.isBedHead(lx, y, lz)) data.beds().add(new ScanData.Bed(new Pos(wx, y, wz), color));
            }
        }
    }
}
