package com.l299l.newbedwars.events.gameplay;

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
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

/** Player deaths (kills, resource transfer, death messages) and team damage. */
public class CombatListener implements Listener {
    private final CombatTracker combat;

    public CombatListener(CombatTracker combat) {
        this.combat = combat;
    }

    @EventHandler
    public void playerDeathEvent(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (ArenaRegistry.byWorld(p.getWorld()) != null) {
            IArena arena = ArenaRegistry.byWorld(p.getWorld());
            if (arena.isPlayerInArena(p) && (arena.status() == GameStatus.playing)) {
                e.setCancelled(true);
                e.setDeathMessage(null);
                boolean mobKill = combat.takeMobHit(p.getUniqueId());
                Player killer = mobKill ? null : p.getKiller();
                if (killer == null) {
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
                EntityDamageEvent lastDamage = p.getLastDamageCause();
                ApiEvents.playerKill(arena, p, killer != null && arena.isPlayerInArena(killer) ? killer : null,
                        dyingTeam != null && dyingTeam.isBedDestroyed(),
                        lastDamage != null ? lastDamage.getCause() : EntityDamageEvent.DamageCause.CUSTOM);
                final String victimName = arena.coloredName(p);
                arena.killPlayer(p);
                if (killer != null && arena.isPlayerInArena(killer)) {
                    final String killerName = arena.coloredName(killer);
                    arena.broadcast("PlayerDeath", Map.of(
                            "/player/", victimName,
                            "/killer/", killerName));
                } else {
                    boolean isVoid = combat.takeVoidDeath(p.getUniqueId());
                    if (!isVoid) {
                        EntityDamageEvent lastDmg = e.getEntity().getLastDamageCause();
                        isVoid = lastDmg != null && lastDmg.getCause() == EntityDamageEvent.DamageCause.VOID;
                    }
                    if (isVoid) {
                        if (lastGround != null && !resources.isEmpty() && lastGround.getWorld() != null) {
                            for (ItemStack stack : resources) {
                                lastGround.getWorld().dropItemNaturally(lastGround, stack);
                            }
                        }
                        arena.broadcast("PlayerDeathByVoid", Map.of("/player/", victimName));
                    } else {
                        EntityDamageEvent lastDmg = e.getEntity().getLastDamageCause();
                        EntityDamageEvent.DamageCause cause = lastDmg != null
                                ? lastDmg.getCause() : EntityDamageEvent.DamageCause.CUSTOM;
                        String causeName = cause.name().toLowerCase().replace('_', ' ');
                        arena.broadcast("PlayerDeath", Map.of(
                                "/player/", victimName,
                                "/killer/", causeName));
                    }
                }
            }
        }
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
