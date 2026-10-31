package com.l299l.newbedwars.arena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The ender dragons of teams with the dragon buff upgrade (sudden death phase). */
class DragonController {
    private final IArena arena;
    private final Map<Team, EnderDragon> dragons = new HashMap<>();
    private final Map<Team, BukkitTask> tasks = new HashMap<>();

    DragonController(IArena arena) {
        this.arena = arena;
    }

    /** Spawns a dragon for every alive team with the dragon buff. @return true when any was spawned */
    boolean spawn() {
        boolean anySpawned = false;
        World world = arena.getArenaWorld();
        for (Team team : arena.getTeams().values()) {
            if (!team.isAlive()) continue;
            TeamUpgrades tu = team.getTeamUpgrades();
            if (tu == null) continue;
            int dragonLevel = tu.getUpgradeLevel(Upgrade.DRAGONBUFF);
            if (dragonLevel < 1) continue;

            Location spawnLoc = team.getTeamSpawn().clone().add(0, Properties.DragonSpawnHeight, 0);
            EnderDragon dragon = (EnderDragon) world.spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
            dragon.setInvulnerable(true);
            dragon.setPhase(EnderDragon.Phase.CIRCLING);
            dragon.setCustomName(team.getColor() + team.getName() + " Dragon");
            dragon.setCustomNameVisible(true);
            dragons.put(team, dragon);
            anySpawned = true;

            final int fireballInterval = (dragonLevel >= 2) ? 10 : 20;
            BukkitTask task = new BukkitRunnable() {
                int fireballTick = 0;

                @Override
                public void run() {
                    EnderDragon d = dragons.get(team);
                    if (d == null || !d.isValid()) {
                        cancel();
                        dragons.remove(team);
                        tasks.remove(team);
                        return;
                    }

                    if (d.getPhase() == EnderDragon.Phase.STRAFING) {
                        d.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
                    }

                    fireballTick++;
                    if (fireballTick < fireballInterval) return;
                    Player target = enemies(team).stream()
                            .min(Comparator.comparingDouble(Player::getHealth))
                            .orElse(null);
                    if (target != null) {
                        fireballTick = 0;
                        DragonFireball fireball = (DragonFireball) world.spawnEntity(
                                d.getLocation().clone().add(0, -1, 0), EntityType.DRAGON_FIREBALL);
                        Vector fbDir = target.getLocation().toVector()
                                .subtract(d.getLocation().toVector()).normalize().multiply(0.5);
                        fireball.setDirection(fbDir);
                        fireball.setShooter(d);
                    }
                }
            }.runTaskTimer(NewBedwars.plugin, 0L, 20L);

            tasks.put(team, task);
        }
        return anySpawned;
    }

    /** Online players of the other alive teams who can be attacked. */
    private List<Player> enemies(Team owningTeam) {
        List<Player> enemies = new ArrayList<>();
        for (Team t : arena.getTeams().values()) {
            if (t == owningTeam || !t.isAlive()) continue;
            for (Player p : t.getPlayers()) {
                if (p.isOnline() && p.getWorld().equals(arena.getArenaWorld())
                        && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
                    enemies.add(p);
                }
            }
        }
        return enemies;
    }

    /** Stops the attack tasks and removes every dragon. */
    void removeAll() {
        for (BukkitTask task : new ArrayList<>(tasks.values())) {
            if (!task.isCancelled()) task.cancel();
        }
        tasks.clear();
        for (EnderDragon dragon : new ArrayList<>(dragons.values())) {
            if (dragon != null && dragon.isValid()) dragon.remove();
        }
        dragons.clear();
    }
}
