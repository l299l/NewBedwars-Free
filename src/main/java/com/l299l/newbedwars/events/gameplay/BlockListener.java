package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.utils.TeamColor;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.SilverfishLogic;
import org.bukkit.entity.*;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

/** Placing and breaking blocks (beds, team blocks), buckets, explosions and setup markers in arena worlds. */
public class BlockListener implements Listener {
    private final NewBedwars plugin;

    public BlockListener(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerPlaceBlockEvent(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null) return;
        if (!arena.isPlayerInArena(p) || arena.status() != GameStatus.playing) return;
        Arena arenaImpl = (Arena) arena;
        if (arenaImpl.isInTeamBase(e.getBlock().getLocation())
                || arenaImpl.isNearDiamondOrEmeraldGenerator(e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }
        Block placed = e.getBlock();
        arena.addPlacedBlock(placed.getLocation());

        CustomItem heldCustom = findCustomItem(p.getInventory().getItemInMainHand());
        if (heldCustom != null && heldCustom.getEvent().getType() == LogicType.BLAST_PROTECTION) {
            arena.addBlastProtBlock(placed.getLocation());
        }

        if (placed.getType() == Material.TNT) {
            placed.setType(Material.AIR, false);
            arena.removeBlastProtBlock(placed.getLocation());
            TNTPrimed tnt = (TNTPrimed) placed.getWorld().spawnEntity(
                    placed.getLocation().add(0.5, 0, 0.5), EntityType.PRIMED_TNT);
            tnt.setFuseTicks(60);
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) return;
        org.bukkit.block.Block target = e.getBlock().getRelative(e.getBlockFace());
        Arena arenaImpl = (Arena) arena;
        if (arenaImpl.isInTeamBase(target.getLocation())
                || arenaImpl.isNearDiamondOrEmeraldGenerator(target.getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void playerBreakBlockEvent(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (ArenaRegistry.byWorld(p.getWorld()) != null) {
            IArena arena = ArenaRegistry.byWorld(p.getWorld());
            if (arena.isPlayerInArena(p) && (arena.status() == GameStatus.playing)) {
                if (e.getBlock().getType().name().toLowerCase().contains("bed")) {
                    Team team = arena.getTeamByBed(e.getBlock().getLocation());
                    if (team != null) {
                        Team playerTeam = arena.getTeam(p);
                        if (playerTeam != null && playerTeam.equals(team)) {
                            e.setCancelled(true);
                            return;
                        }
                        if (!team.isBedDestroyed()) {
                                e.setCancelled(true);
                                if (!ApiEvents.bedBreak(arena, team, p)) return;
                                Block block = e.getBlock();
                                block.setType(Material.AIR, false);
                                for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST,
                                        BlockFace.SOUTH, BlockFace.WEST}) {
                                    Block relative = block.getRelative(face);
                                    if (relative.getType().name().toLowerCase().contains("bed")) {
                                        relative.setType(Material.AIR, false);
                                        break;
                                    }
                                }
                                team.getTeamBed().destroy();
                                GamePlayer breakerGp = arena.getPlayer(p.getUniqueId());
                                if (breakerGp != null) {
                                    breakerGp.addBedBroken();
                                }
                                arena.broadcast("BedDestroyed", Map.of(
                                        "/team/", team.colored(team.getName()),
                                        "/destroyer/", arena.coloredName(p)));
                                for (Player member : team.getPlayers()) {
                                    String titleMsg = ChatColor.translateAlternateColorCodes('&',
                                            plugin.getMessages().getMsg(member, "BedDestroyedTitle"));
                                    String subMsg = ChatColor.translateAlternateColorCodes('&',
                                            plugin.getMessages().getMsg(member, "BedDestroyedSubtitle"));
                                    member.sendTitle(titleMsg, subMsg, 10, 40, 10);
                                }
                        } else {
                            e.setCancelled(true);
                        }
                    } else {
                        e.setCancelled(true);
                    }
                } else if (!arena.isPlacedBlock(e.getBlock().getLocation())) {
                    e.setCancelled(true);
                } else {
                    arena.removeBlastProtBlock(e.getBlock().getLocation());
                    TeamColor.Kind brokenKind = TeamColor.kindOf(e.getBlock().getType());
                    CustomItem blockItem = brokenKind == null ? null : plugin.getCustomItemManager().getTeamBlockItem(brokenKind);
                    if (blockItem != null) {
                        e.setDropItems(false);
                        ItemStack single = blockItem.getItem(p);
                        single.setAmount(1);
                        Map<Integer, ItemStack> leftover = p.getInventory().addItem(single);
                        for (ItemStack drop : leftover.values()) {
                            p.getWorld().dropItem(p.getLocation(), drop);
                        }
                    } else if (brokenKind == TeamColor.Kind.WOOL) {
                        e.setDropItems(false);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (e.getEntity() instanceof EnderDragon
                && com.l299l.newbedwars.config.properties.Properties.DragonIndestructibleBlocks
                .contains(e.getBlock().getType())) {
            e.setCancelled(true);
        }
        // team silverfish never enter or break blocks
        if (e.getEntity() instanceof org.bukkit.entity.Silverfish
                && SilverfishLogic.activeSilverfish.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.DragonFireball) {
            e.blockList().removeIf(block ->
                    com.l299l.newbedwars.config.properties.Properties.DragonIndestructibleBlocks
                            .contains(block.getType()));
            return;
        }
        IArena arena = ArenaRegistry.byWorld(e.getEntity().getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        e.blockList().removeIf(block -> {
            org.bukkit.Location loc = block.getLocation();
            if (!arena.isPlacedBlock(loc)) return true;
            if (arena.isBlastProtBlock(loc)) return true;
            return arena.isBlastProtBlock(loc.clone().add(0, 1, 0));
        });
        // Team blocks (wool, glass, concrete...) must not drop vanilla colored items when exploded: like a
        // normal break they would give a different item than the shop one, so remove them without drops.
        e.blockList().removeIf(block -> {
            TeamColor.Kind kind = TeamColor.kindOf(block.getType());
            if (kind == null || kind == TeamColor.Kind.BED) return false;
            arena.removeBlastProtBlock(block.getLocation());
            block.setType(Material.AIR, false);
            return true;
        });
    }

    /** Removes setup markers that were saved into an arena world (they are re-created when needed). */
    @EventHandler
    public void onArenaEntitiesLoad(org.bukkit.event.world.EntitiesLoadEvent e) {
        if (ArenaRegistry.byWorld(e.getWorld()) == null) return;
        for (org.bukkit.entity.Entity entity : e.getEntities()) {
            if (entity.isPersistent() && DecoUtils.isSetupMarker(entity)) entity.remove();
        }
    }
}
