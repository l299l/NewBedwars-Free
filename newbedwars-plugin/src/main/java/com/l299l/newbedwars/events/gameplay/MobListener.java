package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.dragon.DragonController;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.IronGolemLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.SilverfishLogic;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

public class MobListener implements Listener {

    @EventHandler
    public void onMobDamage(EntityDamageByEntityEvent e) {
        if (golemHitsOwnTeam(e) || isOwnSilverfish(e) || isOwnGolem(e) || dragonHitsOwnTeam(e)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDragonCloud(AreaEffectCloudApplyEvent e) {
        String owner = DragonController.ownerOf(e.getEntity());
        if (owner == null) return;
        e.getAffectedEntities().removeIf(entity -> entity instanceof Player p && isOwnTeam(owner, p));
    }

    private boolean dragonHitsOwnTeam(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return false;
        String owner = DragonController.ownerOf(e.getDamager());
        return owner != null && isOwnTeam(owner, victim);
    }

    private static boolean isOwnTeam(String ownerKey, Player player) {
        MobKey key = MobKey.parse(ownerKey);
        if (key == null) return false;
        IArena arena = key.arena();
        if (arena == null) return false;
        Team team = arena.getTeam(key.teamName());
        return team != null && team.equals(arena.getTeam(player));
    }

    private boolean golemHitsOwnTeam(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof org.bukkit.entity.IronGolem golem)) return false;
        MobKey key = MobKey.parse(IronGolemLogic.getGolemKey(golem));
        if (key == null) return false;
        IArena arena = key.arena();
        if (arena == null) return false;
        Team golemTeam = arena.getTeam(key.teamName());
        if (golemTeam == null) return false;
        return e.getEntity() instanceof Player victim && golemTeam.equals(arena.getTeam(victim));
    }

    private boolean isOwnSilverfish(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.Silverfish sf)) return false;
        if (!SilverfishLogic.activeSilverfish.contains(sf.getUniqueId())) return false;
        return attackedByOwnSide(e, SilverfishLogic.getSilverfishKey(sf), SilverfishLogic.getSfOwner(sf));
    }

    private boolean isOwnGolem(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.IronGolem golem)) return false;
        return attackedByOwnSide(e, IronGolemLogic.getGolemKey(golem),
                golem.getPersistentDataContainer().get(IronGolemLogic.GOLEM_OWNER_KEY, PersistentDataType.STRING));
    }

    private static boolean attackedByOwnSide(EntityDamageByEntityEvent e, String mobKey, String ownerUuid) {
        MobKey key = MobKey.parse(mobKey);
        if (key == null) return false;
        IArena arena = key.arena();
        if (arena == null) return false;
        Team mobTeam = arena.getTeam(key.teamName());
        if (mobTeam == null) return false;
        Player attacker = playerDamager(e.getDamager());
        if (attacker == null) return false;
        if (ownerUuid != null && attacker.getUniqueId().toString().equals(ownerUuid)) return true;
        Team attackerTeam = arena.getTeam(attacker);
        return attackerTeam != null && attackerTeam.equals(mobTeam);
    }

    @EventHandler
    public void onGolemDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.IronGolem golem)) return;
        if (IronGolemLogic.getGolemKey(golem) == null) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        IronGolemLogic.cleanupGolem(golem.getUniqueId().toString());
    }

    @EventHandler
    public void onGolemTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.IronGolem)) return;
        MobKey key = MobKey.parse(IronGolemLogic.getGolemKey(e.getEntity()));
        if (key == null) return;
        IArena arena = key.arena();
        if (arena == null) { e.setCancelled(true); return; }
        if (e.getTarget() instanceof Player target) {
            Team golemTeam = arena.getTeam(key.teamName());
            Team targetTeam = arena.getTeam(target);
            if (golemTeam != null && golemTeam.equals(targetTeam)) {
                e.setCancelled(true);
                return;
            }
            if (!arena.isPlayerInArena(target)) {
                e.setCancelled(true);
            }
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSilverfishTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.Silverfish)) return;
        if (!SilverfishLogic.activeSilverfish.contains(e.getEntity().getUniqueId())) return;
        MobKey key = MobKey.parse(SilverfishLogic.getSilverfishKey(e.getEntity()));
        if (key == null) { e.setCancelled(true); return; }
        IArena arena = key.arena();
        if (arena == null) { e.setCancelled(true); return; }
        if (!(e.getTarget() instanceof Player target)) {
            e.setCancelled(true);
            return;
        }

        String ownerUuid = SilverfishLogic.getSfOwner(e.getEntity());
        if (ownerUuid != null && target.getUniqueId().toString().equals(ownerUuid)) {
            e.setCancelled(true);
            return;
        }

        Team sfTeam = arena.getTeam(key.teamName());
        Team targetTeam = arena.getTeam(target);
        if (sfTeam == null || targetTeam == null || sfTeam.equals(targetTeam)) {
            e.setCancelled(true);
        }
    }
}
