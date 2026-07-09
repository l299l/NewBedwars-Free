package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PortableTowerLogic implements CustomLogic {

    @Override
    public void perform(Player player, IArena arena) {
        Team team = arena.getTeam(player);
        if (team == null) return;

        Material wool = BridgeEggLogic.getWoolFromColor(team.getColor());
        org.bukkit.World world = arena.getArenaWorld();

        Block target = player.getTargetBlock(null, 6);
        int cx, floorY, cz;
        if (target == null || target.getType().isAir()) {
            cx = player.getLocation().getBlockX();
            floorY = player.getLocation().getBlockY() - 1;
            cz = player.getLocation().getBlockZ();
        } else {
            cx = target.getX();
            floorY = target.getY();
            cz = target.getZ();
        }

        if (arena instanceof Arena arenaImpl) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 0; dy <= 6; dy++) {
                        Location check = new Location(world, cx + dx, floorY + dy, cz + dz);
                        if (arenaImpl.isInTeamBase(check) || arenaImpl.isNearDiamondOrEmeraldGenerator(check)) {
                            NewBedwars.plugin.getMessages().send(player, "PortableTowerProtectedArea");
                            return; // item NOT consumed — caller skips consumeOneItem for PORTABLE_TOWER
                        }
                    }
                }
            }
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }

        // Entrance faces back towards where the player was standing when they placed the
        // tower (the opposite of their look direction), not away from them.
        BlockFace facing = getCardinalDirection(player.getLocation().getYaw()).getOppositeFace();
        int doorDx = facing.getModX();
        int doorDz = facing.getModZ();

        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                setTracked(arena, wool, world, cx + dx, floorY, cz + dz);

        for (int dy = 1; dy <= 4; dy++) {
            int y = floorY + dy;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        Block lb = world.getBlockAt(cx, y, cz);
                        if (!lb.getType().isAir() && !arena.isPlacedBlock(lb.getLocation())) continue;
                        lb.setType(Material.LADDER, false);
                        Ladder ld = (Ladder) lb.getBlockData();
                        ld.setFacing(facing);
                        lb.setBlockData(ld, false);
                        arena.addPlacedBlock(lb.getLocation());
                    } else if (dx == doorDx && dz == doorDz && dy <= 2) {
                        // Doorway facing the player's direction on placement — air for entry/exit
                    } else {
                        setTracked(arena, wool, world, cx + dx, y, cz + dz);
                    }
                }
            }
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue; // roof hatch directly above ladder
                setTracked(arena, wool, world, cx + dx, floorY + 5, cz + dz);
            }
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean onEdge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                boolean evenPos = dx % 2 == 0 && dz % 2 == 0;
                if (onEdge && evenPos) {
                    setTracked(arena, wool, world, cx + dx, floorY + 6, cz + dz);
                }
            }
        }
    }

    private BlockFace getCardinalDirection(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    private void setTracked(IArena arena, Material mat, org.bukkit.World world, int x, int y, int z) {
        Location loc = new Location(world, x, y, z);
        Block b = loc.getBlock();
        if (!b.getType().isAir() && !arena.isPlacedBlock(loc)) return;
        b.setType(mat, false);
        arena.addPlacedBlock(loc);
    }

    @Override
    public LogicType getType() { return LogicType.PORTABLE_TOWER; }
}
