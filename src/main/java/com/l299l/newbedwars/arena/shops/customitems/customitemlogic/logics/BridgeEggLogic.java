package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class BridgeEggLogic implements CustomLogic {

    public static final HashMap<UUID, Material> pendingBridgeEggs = new HashMap<>();

    @Override
    public void perform(Player player, IArena arena) {
    }

    public static void startTracking(UUID eggId, Material woolType, IArena arena, Egg egg) {
        pendingBridgeEggs.put(eggId, woolType);
        final Location[] prevLoc = {egg.getLocation().clone()};
        placeWool(prevLoc[0], woolType, arena);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!egg.isValid()) {
                    placeWool(egg.getLocation(), woolType, arena);
                    pendingBridgeEggs.remove(eggId);
                    cancel();
                    return;
                }
                Location current = egg.getLocation();
                if (current.getBlockX() != prevLoc[0].getBlockX()
                        || current.getBlockZ() != prevLoc[0].getBlockZ()) {
                    placeWool(current, woolType, arena);
                    prevLoc[0] = current.clone();
                }
            }
        }.runTaskTimer(NewBedwars.plugin, 1L, 1L);
    }

    private static void placeWool(Location loc, Material woolType, IArena arena) {
        int baseY = loc.getBlockY() - 1;
        com.l299l.newbedwars.arena.Arena arenaImpl = arena instanceof com.l299l.newbedwars.arena.Arena a ? a : null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Location wLoc = new Location(loc.getWorld(), loc.getBlockX() + dx, baseY, loc.getBlockZ() + dz);
                if (arenaImpl != null && (arenaImpl.isInTeamBase(wLoc)
                        || arenaImpl.isNearDiamondOrEmeraldGenerator(wLoc))) {
                    continue;
                }
                Block b = wLoc.getBlock();
                if (!b.getType().isSolid()) {
                    b.setType(woolType, false);
                    arena.addPlacedBlock(wLoc);
                }
            }
        }
    }

    public static Material getWoolFromColor(ChatColor color) {
        return switch (color) {
            case RED, DARK_RED -> Material.RED_WOOL;
            case BLUE, DARK_BLUE -> Material.BLUE_WOOL;
            case GREEN, DARK_GREEN -> Material.GREEN_WOOL;
            case YELLOW -> Material.YELLOW_WOOL;
            case AQUA -> Material.LIGHT_BLUE_WOOL;
            case BLACK -> Material.BLACK_WOOL;
            case DARK_GRAY -> Material.GRAY_WOOL;
            case GRAY -> Material.LIGHT_GRAY_WOOL;
            case DARK_AQUA -> Material.CYAN_WOOL;
            case DARK_PURPLE -> Material.PURPLE_WOOL;
            case GOLD -> Material.ORANGE_WOOL;
            case LIGHT_PURPLE -> Material.MAGENTA_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    @Override
    public LogicType getType() {
        return LogicType.BRIDGE_EGG;
    }
}
