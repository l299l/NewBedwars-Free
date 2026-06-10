package com.l299l.newbedwars.events;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.BridgeEggLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.NoneLogic;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.version.VersionCompat;
import com.l299l.newbedwars.gui.configuration.game.guis.spectator.SpectatorEffectsGUI;
import com.l299l.newbedwars.gui.configuration.game.guis.spectator.SpectatorPlayersGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ArenaGameplayEvents implements Listener {
    private final NewBedwars plugin;
    private final HashMap<UUID, BukkitTask> pendingLeave = new HashMap<>();
    private final HashMap<UUID, UUID> lastAttacker = new HashMap<>();
    private final HashMap<UUID, BukkitTask> lastAttackerClearTasks = new HashMap<>();
    private final HashSet<UUID> quickVoidDeaths = new HashSet<>();
    private final HashSet<UUID> pendingBridgeEggThrow = new HashSet<>();
    private final HashMap<UUID, Set<String>> playersInBases = new HashMap<>();

    public ArenaGameplayEvents(NewBedwars plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        BukkitTask pending = pendingLeave.remove(p.getUniqueId());
        if (pending != null) pending.cancel();
        lastAttacker.remove(p.getUniqueId());
        BukkitTask clearTask = lastAttackerClearTasks.remove(p.getUniqueId());
        if (clearTask != null) clearTask.cancel();
        quickVoidDeaths.remove(p.getUniqueId());
        pendingBridgeEggThrow.remove(p.getUniqueId());
        playersInBases.remove(p.getUniqueId());
        if(Arena.arenaByWorld.get(p.getWorld()) != null) {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
            if(arena.isPlayerInArena(p)) {
                if (arena.status() == GameStatus.starting) {
                    arena.cancelStart();
                    arena.leave(p);
                } else if (arena.status() == GameStatus.waiting || arena.status() == GameStatus.playing
                        || arena.status() == GameStatus.ending) {
                    arena.leave(p);
                }
            } else if (arena.getSpectators().contains(p)) {
                arena.leave(p);
            }
        }
    }

    @EventHandler
    public void playerQuitRequest(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (Arena.arenaByWorld.get(p.getWorld()) == null) return;
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (!arena.isPlayerInArena(p) && !arena.getSpectators().contains(p)) return;
        if (e.getHand() != null && e.getHand() != EquipmentSlot.HAND) return;
        Material held = p.getInventory().getItemInMainHand().getType();

        if (arena.isPlayerInArena(p) && (arena.status() == GameStatus.waiting || arena.status() == GameStatus.starting)) {
            if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (held.equals(plugin.getGuiManager().getLeaveItem(p).getType())) {
                e.setCancelled(true);
                handleBedLeave(p, arena, false);
            }
            return;
        }

        if ((arena.status() == GameStatus.playing || arena.status() == GameStatus.ending)
                && arena.getSpectators().contains(p)) {
            // Cancel ALL spectator interactions to prevent default item behaviour;
            // only open GUIs on right-click.
            e.setCancelled(true);
            if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (held == Material.RED_BED) {
                handleBedLeave(p, arena, true);
            } else if (held == Material.COMPASS) {
                p.openInventory(new SpectatorPlayersGUI(plugin.getGuiManager(), p, arena).getInventory());
            } else if (held == Material.COMPARATOR) {
                p.openInventory(new SpectatorEffectsGUI(plugin.getGuiManager(), p).getInventory());
            }
        }
    }

    private void handleBedLeave(Player p, IArena arena, boolean isSpectator) {
        if (!Properties.BedLeaveConfirmEnabled) {
            doLeave(p, arena, isSpectator);
            return;
        }
        if (pendingLeave.containsKey(p.getUniqueId())) {
            pendingLeave.remove(p.getUniqueId()).cancel();
            plugin.getMessages().send(p, "BedLeaveCancelled");
            return;
        }
        int delay = Properties.BedLeaveConfirmDelay;
        int[] secondsLeft = {delay};
        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || (!arena.isPlayerInArena(p) && !arena.getSpectators().contains(p))) {
                    pendingLeave.remove(p.getUniqueId());
                    cancel();
                    return;
                }
                if (secondsLeft[0] <= 0) {
                    pendingLeave.remove(p.getUniqueId());
                    cancel();
                    doLeave(p, arena, isSpectator);
                    return;
                }
                String msg = plugin.getMessages().getMsg(p, "BedLeaveConfirmTitle")
                        .replace("/seconds/", String.valueOf(secondsLeft[0]));
                String sub = plugin.getMessages().getMsg(p, "BedLeaveConfirmSubtitle");
                p.sendMessage(msg + " " + sub);
                secondsLeft[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        pendingLeave.put(p.getUniqueId(), task);
    }

    private void doLeave(Player p, IArena arena, boolean isSpectator) {
        if (!isSpectator && arena.status() == GameStatus.starting) {
            arena.cancelStart();
        }
        arena.leave(p);
    }

    @EventHandler
    public void playerDropItem(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (Arena.arenaByWorld.get(p.getWorld()) != null) {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
            if (arena.getSpectators().contains(p) ||
                    (arena.isPlayerInArena(p) && (arena.status() == GameStatus.waiting || arena.status() == GameStatus.starting))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (arena == null) return;
        if (arena.getSpectators().contains(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void playerInvChange(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getInventory().getHolder() instanceof ArenaSelectGUI gui) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;
            IArena target = gui.getArena(e.getRawSlot());
            if (target == null) return;
            p.closeInventory();
            GameStatus status = target.status();
            if (status != GameStatus.waiting && status != GameStatus.starting) {
                plugin.getMessages().send(p, "ArenaRunningError");
                return;
            }
            if (!target.join(p)) {
                plugin.getMessages().send(p, "ArenaIsFullError");
            }
            return;
        }

        if (Arena.arenaByWorld.get(p.getWorld()) != null) {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
            if (arena.getSpectators().contains(p) ||
                    (arena.isPlayerInArena(p) && (arena.status() == GameStatus.waiting || arena.status() == GameStatus.starting))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void playerNpcInteract(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (Arena.arenaByWorld.get(p.getWorld()) != null) {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
            if (arena.isPlayerInArena(p) && (arena.status() == GameStatus.playing)) {
                List<TeamShop> teamShops = arena.getTeamShops();
                List<TeamUpgrades> teamUpgrades = arena.getTeamUpgrades();
                Team team = arena.getTeam(p);
                for (TeamShop shop : teamShops) {
                    if (e.getRightClicked().getUniqueId().equals(shop.getNpc().getUniqueId())) {
                        team.getTeamShop().open(p);
                        return;
                    }
                }
                for (TeamUpgrades upgrade : teamUpgrades) {
                    if (e.getRightClicked().getUniqueId().equals(upgrade.getNpc().getUniqueId())) {
                        team.getTeamUpgrades().open(p);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void playerDeathEvent(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (Arena.arenaByWorld.get(p.getWorld()) != null) {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
            if (arena.isPlayerInArena(p) && (arena.status() == GameStatus.playing)) {
                e.setCancelled(true);
                e.setDeathMessage(null);
                Player killer = p.getKiller();
                if (killer == null) {
                    UUID attackerId = lastAttacker.get(p.getUniqueId());
                    if (attackerId != null) killer = Bukkit.getPlayer(attackerId);
                }
                if (killer != null && arena.isPlayerInArena(killer)) {
                    GamePlayer killerGp = arena.getPlayer(killer.getUniqueId());
                    if (killerGp != null) {
                        killerGp.addKill();
                        Team victimTeam = arena.getTeam(p);
                        if (victimTeam != null && victimTeam.isBedDestroyed()) {
                            killerGp.addFinalKill();
                        }
                    }
                    final Player finalKiller = killer;
                    for (ItemStack stack : p.getInventory().getContents()) {
                        if (stack == null) continue;
                        Material m = stack.getType();
                        if (m == Material.IRON_INGOT || m == Material.GOLD_INGOT
                                || m == Material.DIAMOND || m == Material.EMERALD) {
                            finalKiller.getInventory().addItem(stack.clone());
                        }
                    }
                }
                arena.killPlayer(p);
                if (killer != null && arena.isPlayerInArena(killer)) {
                    final String killerName = killer.getName();
                    arena.broadcast("PlayerDeath", new HashMap<>() {{
                        put("/player/", p.getName());
                        put("/killer/", killerName);
                    }});
                } else {
                    boolean isVoid = quickVoidDeaths.remove(p.getUniqueId());
                    if (!isVoid) {
                        EntityDamageEvent lastDmg = e.getEntity().getLastDamageCause();
                        isVoid = lastDmg != null && lastDmg.getCause() == EntityDamageEvent.DamageCause.VOID;
                    }
                    if (isVoid) {
                        arena.broadcast("PlayerDeathByVoid", new HashMap<>() {{
                            put("/player/", p.getName());
                        }});
                    } else {
                        EntityDamageEvent lastDmg = e.getEntity().getLastDamageCause();
                        EntityDamageEvent.DamageCause cause = lastDmg != null
                                ? lastDmg.getCause() : EntityDamageEvent.DamageCause.CUSTOM;
                        String causeName = cause.name().toLowerCase().replace('_', ' ');
                        arena.broadcast("PlayerDeath", new HashMap<>() {{
                            put("/player/", p.getName());
                            put("/killer/", causeName);
                        }});
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTeamDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        IArena arena = Arena.arenaByWorld.get(victim.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (arena.getSpectators().contains(victim)) {
            e.setCancelled(true);
            return;
        }
        if (!arena.isPlayerInArena(victim)) return;
        if (arena.getGamerules() != null && arena.getGamerules().AllowTeamDamage) return;
        Player attacker = null;
        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker != null && arena.getSpectators().contains(attacker)) {
            e.setCancelled(true);
            return;
        }
        if (attacker == null || !arena.isPlayerInArena(attacker)) return;
        Team victimTeam = arena.getTeam(victim);
        Team attackerTeam = arena.getTeam(attacker);
        if (victimTeam != null && victimTeam.equals(attackerTeam)) {
            e.setCancelled(true);
            return;
        }
        recordAttacker(victim.getUniqueId(), attacker.getUniqueId());
    }

    private void recordAttacker(UUID victimId, UUID attackerId) {
        lastAttacker.put(victimId, attackerId);
        BukkitTask prev = lastAttackerClearTasks.remove(victimId);
        if (prev != null) prev.cancel();
        BukkitTask clear = new BukkitRunnable() {
            @Override public void run() {
                lastAttacker.remove(victimId);
                lastAttackerClearTasks.remove(victimId);
            }
        }.runTaskLater(plugin, 200L);
        lastAttackerClearTasks.put(victimId, clear);
    }

    @EventHandler
    public void playerPlaceBlockEvent(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
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
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) return;
        org.bukkit.block.Block target = e.getBlock().getRelative(e.getBlockFace());
        Arena arenaImpl = (Arena) arena;
        if (arenaImpl.isInTeamBase(target.getLocation())
                || arenaImpl.isNearDiamondOrEmeraldGenerator(target.getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getTo() == null || e.getFrom().getBlockY() == e.getTo().getBlockY()) return;
        Player p = e.getPlayer();
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        Integer voidY = arena.getQuickVoidY();
        if (voidY == null || e.getTo().getY() > voidY) return;
        if (arena.getSpectators().contains(p)) {
            p.teleport(arena.getWaitingSpawn());
            return;
        }
        if (!arena.isPlayerInArena(p)) return;
        UUID attackerId = lastAttacker.get(p.getUniqueId());
        if (attackerId != null) {
            Player attacker = Bukkit.getPlayer(attackerId);
            if (attacker != null && attacker.isOnline() && arena.isPlayerInArena(attacker)) {
                p.damage(p.getHealth() + 1, attacker);
                return;
            }
        }
        quickVoidDeaths.add(p.getUniqueId());
        p.damage(p.getHealth() + 1);
    }

    @EventHandler
    public void playerBreakBlockEvent(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (Arena.arenaByWorld.get(p.getWorld()) != null) {
            IArena arena = Arena.arenaByWorld.get(p.getWorld());
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
                                arena.broadcast("BedDestroyed", new HashMap<>() {{
                                    put("/team/", team.getName());
                                    put("/destroyer/", p.getName());
                                }});
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
                }
            }
        }
    }

    @EventHandler
    public void onDragonBlockBreak(EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof EnderDragon)) return;
        if (com.l299l.newbedwars.config.properties.Properties.DragonIndestructibleBlocks
                .contains(e.getBlock().getType())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDragonFireballExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.DragonFireball)) return;
        e.blockList().removeIf(block ->
                com.l299l.newbedwars.config.properties.Properties.DragonIndestructibleBlocks
                        .contains(block.getType()));
    }

    @EventHandler
    public void onArenaExplosion(EntityExplodeEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.DragonFireball) return;
        IArena arena = Arena.arenaByWorld.get(e.getEntity().getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        e.blockList().removeIf(block ->
                !arena.isPlacedBlock(block.getLocation()) || arena.isBlastProtBlock(block.getLocation()));
    }

    @EventHandler
    public void onCustomItemInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        CustomItem customItem = findCustomItem(item);
        if (customItem == null || customItem.getEvent() instanceof NoneLogic) return;

        LogicType logicType = customItem.getEvent().getType();
        switch (logicType) {
            case BRIDGE_EGG -> {
                pendingBridgeEggThrow.add(p.getUniqueId());
            }
            case FIREBALL -> {
                e.setCancelled(true);
                customItem.getEvent().perform(p, arena);
                consumeOneItem(p);
            }
            case POTION -> {
                e.setCancelled(true);
                PotionEffect effect = getPotionEffect(customItem.getName());
                if (effect != null) p.addPotionEffect(effect);
                consumeOneItem(p);
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Egg egg)) return;
        if (!(egg.getShooter() instanceof Player p)) return;
        if (!pendingBridgeEggThrow.remove(p.getUniqueId())) return;

        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        Team team = arena.getTeam(p);
        if (team == null) return;

        Material woolType = BridgeEggLogic.getWoolFromColor(team.getColor());
        BridgeEggLogic.startTracking(egg.getUniqueId(), woolType, arena, egg);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMoveForTraps(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player p = e.getPlayer();
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) return;

        Team playerTeam = arena.getTeam(p);
        if (playerTeam == null) return;

        Set<String> currentBases = playersInBases.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());

        for (Team team : arena.getTeams().values()) {
            if (team.equals(playerTeam) || !team.isAlive()) continue;
            boolean wasIn = currentBases.contains(team.getName());
            boolean isIn = team.isInBase(e.getTo());
            if (isIn && !wasIn) {
                currentBases.add(team.getName());
                TeamUpgrades tu = team.getTeamUpgrades();
                if (tu != null) tu.triggerTraps(p, arena);
            } else if (!isIn && wasIn) {
                currentBases.remove(team.getName());
            }
        }
    }

    private CustomItem findCustomItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getItemMeta() == null) return null;
        NamespacedKey key = new NamespacedKey(plugin, "custom_item_name");
        String name = stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (name == null) return null;
        return plugin.getCustomItemManager().getCustomItem(name);
    }

    @EventHandler
    public void onCustomPotionConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        IArena arena = Arena.arenaByWorld.get(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p)) return;
        CustomItem customItem = findCustomItem(e.getItem());
        if (customItem != null && customItem.getEvent().getType() == LogicType.POTION) {
            e.setCancelled(true);
            return;
        }
        if (e.getItem().getType() == Material.POTION) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand != null && inHand.getType() == Material.GLASS_BOTTLE) {
                    p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    p.updateInventory();
                }
            }, 1L);
        }
    }

    private PotionEffect getPotionEffect(String itemName) {
        return switch (itemName.toLowerCase()) {
            case "speed"        -> new PotionEffect(PotionEffectType.SPEED,        200, 1, true, true);
            case "jump"         -> new PotionEffect(VersionCompat.JUMP_BOOST,      200, 1, true, true);
            case "inviscibility"-> new PotionEffect(PotionEffectType.INVISIBILITY, 600, 0, true, true);
            default -> null;
        };
    }

    private void consumeOneItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }
}
