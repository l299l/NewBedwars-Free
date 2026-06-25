package com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics;

import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.ChatColor;
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
                            player.sendMessage(ChatColor.RED + "Cannot place tower here — protected area!");
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
                        ld.setFacing(BlockFace.SOUTH);
                        lb.setBlockData(ld, false);
                        arena.addPlacedBlock(lb.getLocation());
                    } else if (dx == 0 && dz == 1 && dy <= 2) {
                        // South doorway — air for entry/exit
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
