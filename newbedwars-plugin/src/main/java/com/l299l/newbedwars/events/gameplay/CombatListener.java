package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.IronGolemLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.SilverfishLogic;
import org.bukkit.entity.*;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

public class CombatListener implements Listener {
    private final CombatTracker combat;

    public CombatListener(CombatTracker combat) {
        this.combat = combat;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) return;
        boolean isVoid = e.getCause() == EntityDamageEvent.DamageCause.VOID;
        if (!isVoid && (e.getFinalDamage() < p.getHealth() || holdsTotem(p))) return;
        e.setCancelled(true);
        if (isVoid) combat.markVoidDeath(p.getUniqueId());
        Player direct = e instanceof EntityDamageByEntityEvent byEntity ? playerDamager(byEntity.getDamager()) : null;
        handleDeath(p, arena, e, direct);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerDeathEvent(PlayerDeathEvent e) {
        Player p = e.getEntity();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null) return;
        boolean spectator = arena.getSpectators().contains(p);
        if (!arena.isPlayerInArena(p) && !spectator) return;
        e.setCancelled(true);
        e.setDeathMessage(null);
        e.setReviveHealth(p.getMaxHealth());
        p.setHealth(p.getMaxHealth());
        if (arena.status() == GameStatus.playing && !spectator) {
            handleDeath(p, arena, p.getLastDamageCause(), null);
        } else {
            rescueFromVoid(p, arena);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || (!arena.isPlayerInArena(p) && !arena.getSpectators().contains(p))) return;
        Location spawn = arena.getWaitingSpawn();
        if (spawn == null) return;
        e.setRespawnLocation(spawn);
        Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
            if (!p.isOnline() || ArenaRegistry.byWorld(p.getWorld()) != arena) return;
            if (arena.getSpectators().contains(p)) {
                p.setAllowFlight(true);
                p.setFlying(true);
                arena.ensureRespawning(p);
            } else if (arena.status() == GameStatus.playing && arena.isPlayerInArena(p)) {
                handleDeath(p, arena, p.getLastDamageCause(), null);
            }
        });
    }

    void voidDeath(Player p, IArena arena) {
        combat.markVoidDeath(p.getUniqueId());
        UUID attackerId = combat.lastAttacker(p.getUniqueId());
        Player attacker = attackerId != null ? Bukkit.getPlayer(attackerId) : null;
        if (attacker != null && (!attacker.isOnline() || !arena.isPlayerInArena(attacker))) attacker = null;
        handleDeath(p, arena, null, attacker);
    }

    void handleDeath(Player p, IArena arena, EntityDamageEvent lastDamage, Player directKiller) {
        if (!arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) return;
        if (arena.getTeam(p) == null) {
            combat.takeVoidDeath(p.getUniqueId());
            rescueFromVoid(p, arena);
            return;
        }
        boolean mobKill = combat.takeMobHit(p.getUniqueId());
        Team dragonTeam = dragonTeam(arena, lastDamage);
        Player killer = null;
        if (!mobKill && dragonTeam == null) killer = directKiller != null ? directKiller : p.getKiller();
        if (killer == null && dragonTeam == null) {
            UUID attackerId = combat.lastAttacker(p.getUniqueId());
            if (attackerId != null) killer = Bukkit.getPlayer(attackerId);
        }
        List<ItemStack> resources = new ArrayList<>();
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack == null) continue;
            Material m = stack.getType();
            if (m == Material.IRON_INGOT || m == Material.GOLD_INGOT
                    || m == Material.DIAMOND || m == Material.EMERALD) {
                resources.add(stack.clone());
            }
        }
        Location lastGround = combat.takeGround(p.getUniqueId());
        if (killer != null && arena.isPlayerInArena(killer)) {
            GamePlayer killerGp = arena.getPlayer(killer.getUniqueId());
            if (killerGp != null) {
                killerGp.addKill();
                Team victimTeam = arena.getTeam(p);
                if (victimTeam != null && victimTeam.isBedDestroyed()) {
                    killerGp.addFinalKill();
                }
            }
            if (mobKill) {
                Location dropAt = lastGround != null && lastGround.getWorld() != null
                        && p.getLocation().getY() < p.getWorld().getMinHeight()
                        ? lastGround : p.getLocation();
                for (ItemStack stack : resources) {
                    dropAt.getWorld().dropItemNaturally(dropAt, stack);
                }
            } else {
                final Player finalKiller = killer;
                for (ItemStack stack : resources) {
                    finalKiller.getInventory().addItem(stack);
                }
            }
        }
        GamePlayer dyingGp = arena.getPlayer(p.getUniqueId());
        if (dyingGp != null) dyingGp.addDeath();
        Team dyingTeam = arena.getTeam(p);
        EntityDamageEvent.DamageCause cause = deathCause(lastDamage, combat.takeVoidDeath(p.getUniqueId()));
        boolean isVoid = cause == EntityDamageEvent.DamageCause.VOID;
        ApiEvents.playerKill(arena, p, killer != null && arena.isPlayerInArena(killer) ? killer : null,
                dyingTeam != null && dyingTeam.isBedDestroyed(), cause, dragonTeam);
        final String victimName = arena.coloredName(p);
        arena.killPlayer(p);
        if (killer != null && arena.isPlayerInArena(killer)) {
            final String killerName = arena.coloredName(killer);
            arena.broadcast("PlayerDeath", Map.of(
                    "/player/", victimName,
                    "/killer/", killerName));
        } else if (isVoid) {
            if (lastGround != null && !resources.isEmpty() && lastGround.getWorld() != null) {
                for (ItemStack stack : resources) {
                    lastGround.getWorld().dropItemNaturally(lastGround, stack);
                }
            }
            arena.broadcast("PlayerDeathByVoid", Map.of("/player/", victimName));
        } else if (dragonTeam != null) {
            arena.broadcast("PlayerDeathByDragon", Map.of(
                    "/player/", victimName,
                    "/team/", dragonTeam.getColor() + dragonTeam.getName()));
        } else {
            String causeKey = "DeathCause-" + cause.name();
            arena.broadcastEach("PlayerDeath", viewer -> Map.of(
                    "/player/", victimName,
                    "/killer/", causeText(viewer, causeKey)));
        }
    }

    static EntityDamageEvent.DamageCause deathCause(EntityDamageEvent lastDamage, boolean quickVoid) {
        if (quickVoid) return EntityDamageEvent.DamageCause.VOID;
        return lastDamage != null ? lastDamage.getCause() : EntityDamageEvent.DamageCause.CUSTOM;
    }

    private static boolean holdsTotem(Player p) {
        return p.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || p.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }

    private static String causeText(Player viewer, String key) {
        String text = NewBedwars.plugin.getMessages().getMsg(viewer, key);
        return text.isEmpty() ? NewBedwars.plugin.getMessages().getMsg(viewer, "DeathCause-OTHER") : text;
    }

    @EventHandler
    public void onTeamDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        IArena arena = ArenaRegistry.byWorld(victim.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (arena.getSpectators().contains(victim)) {
            e.setCancelled(true);
            return;
        }
        if (!arena.isPlayerInArena(victim)) return;
        if (arena.getGamerules() != null && arena.getGamerules().AllowTeamDamage) return;
        Player attacker = playerDamager(e.getDamager());
        boolean byMob = false;
        if (attacker == null) {
            String ownerUuid = null;
            if (e.getDamager() instanceof org.bukkit.entity.IronGolem ig
                    && IronGolemLogic.getGolemKey(ig) != null) {
                ownerUuid = ig.getPersistentDataContainer().get(
                        IronGolemLogic.GOLEM_OWNER_KEY, PersistentDataType.STRING);
            } else if (e.getDamager() instanceof org.bukkit.entity.Silverfish sf
                    && SilverfishLogic.activeSilverfish.contains(sf.getUniqueId())) {
                ownerUuid = SilverfishLogic.getSfOwner(sf);
            }
            byMob = true;
            if (ownerUuid != null) {
                try { attacker = Bukkit.getPlayer(java.util.UUID.fromString(ownerUuid)); }
                catch (IllegalArgumentException ignored) {}
            }
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
        combat.record(victim.getUniqueId(), attacker.getUniqueId(), byMob);
    }
}
