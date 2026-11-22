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
import org.bukkit.event.entity.ProjectileHitEvent;
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
        Player direct = null;
        if (e instanceof EntityDamageByEntityEvent byEntity) {
            direct = playerDamager(byEntity.getDamager());
            recordCredit(arena, p, byEntity);
        }
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
        if (killer == p || (killer != null && p.getUniqueId() != null && p.getUniqueId().equals(killer.getUniqueId()))) killer = null;
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
        combat.clearAttacker(p.getUniqueId());
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
                    com.l299l.newbedwars.arena.player.inventory.ArmorContents.giveOrDrop(finalKiller, stack);
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
                    "/team/", dragonTeam.getColor() + dragonTeam.getDisplayName()));
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
        if (e.getDamager() instanceof org.bukkit.entity.TNTPrimed) return;
        Attack attack = attackOf(e.getDamager());
        Player attacker = attack.attacker();
        if (attacker != null && arena.getSpectators().contains(attacker)) {
            e.setCancelled(true);
            return;
        }
        if (attacker == null || !arena.isPlayerInArena(attacker)) return;
        boolean explosive = e.getDamager() instanceof Projectile && e.getDamager() instanceof Explosive;
        if (explosive && attacker.equals(victim)) return;
        if (!isEnemy(arena, victim, attacker)) {
            if (arena.getGamerules() == null || !arena.getGamerules().AllowTeamDamage) {
                if (explosive) e.setDamage(0);
                else e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHitSpectator(ProjectileHitEvent e) {
        if (!(e.getHitEntity() instanceof Player hit)) return;
        IArena arena = ArenaRegistry.byWorld(hit.getWorld());
        if (arena != null && arena.getSpectators().contains(hit)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageCredit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        IArena arena = ArenaRegistry.byWorld(victim.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (arena.getSpectators().contains(victim) || !arena.isPlayerInArena(victim)) return;
        recordCredit(arena, victim, e);
    }

    private void recordCredit(IArena arena, Player victim, EntityDamageByEntityEvent e) {
        Attack attack = attackOf(e.getDamager());
        Player attacker = attack.attacker();
        if (attacker == null || attacker.equals(victim) || !isEnemy(arena, victim, attacker)) return;
        combat.record(victim.getUniqueId(), attacker.getUniqueId(), attack.byMob());
    }

    private record Attack(Player attacker, boolean byMob) {}

    private static Attack attackOf(Entity damager) {
        if (damager instanceof org.bukkit.entity.TNTPrimed tnt) {
            return new Attack(tnt.getSource() instanceof Player owner ? owner : null, false);
        }
        Player attacker = playerDamager(damager);
        if (attacker != null) return new Attack(attacker, false);
        String ownerUuid = null;
        if (damager instanceof org.bukkit.entity.IronGolem ig && IronGolemLogic.getGolemKey(ig) != null) {
            ownerUuid = ig.getPersistentDataContainer().get(IronGolemLogic.GOLEM_OWNER_KEY, PersistentDataType.STRING);
        } else if (damager instanceof org.bukkit.entity.Silverfish sf
                && SilverfishLogic.activeSilverfish.contains(sf.getUniqueId())) {
            ownerUuid = SilverfishLogic.getSfOwner(sf);
        }
        if (ownerUuid != null) {
            try {
                return new Attack(Bukkit.getPlayer(UUID.fromString(ownerUuid)), true);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new Attack(null, true);
    }

    private static boolean isEnemy(IArena arena, Player victim, Player attacker) {
        if (!arena.isPlayerInArena(attacker) || arena.getSpectators().contains(attacker)) return false;
        Team victimTeam = arena.getTeam(victim);
        return victimTeam == null || !victimTeam.equals(arena.getTeam(attacker));
    }
}
