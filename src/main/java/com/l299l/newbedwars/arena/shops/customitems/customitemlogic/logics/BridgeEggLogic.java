package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.utils.TeamColor;
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
    public boolean perform(Player player, IArena arena) {
        return true; // Bridge egg logic handled by projectile launch
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

    @Override
    public LogicType getType() {
        return LogicType.BRIDGE_EGG;
    }
}
