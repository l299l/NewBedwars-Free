package com.l299l.newbedwars.arena.dragon;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Candidates;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Kind;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Target;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.arena.team.TeamBed;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DragonController {
    private static NamespacedKey ownerKey;

    private final IArena arena;
    private final Map<Team, TeamDragon> dragons = new LinkedHashMap<>();
    private final Map<UUID, Vector> lastPositions = new HashMap<>();
    private DragonBounds bounds;
    private BukkitTask task;

    public DragonController(IArena arena) {
        this.arena = arena;
    }

    public boolean spawn() {
        World world = arena.getArenaWorld();
        if (world == null) return false;
        DragonSettings settings = Properties.Dragon;
        if (bounds == null) {
            bounds = DragonBounds.of(groundPoints(), basePoints(), settings.margin(), world.getMinHeight(),
                    world.getMaxHeight(), settings.patrolHeight(), settings.patrolRadius());
        }
        if (bounds == null) return false;
        boolean anySpawned = false;
        for (Team team : arena.getTeams().values()) {
            if (dragons.containsKey(team) || !team.isAlive() || !hasOnlinePlayer(team, world)) continue;
            TeamUpgrades upgrades = team.getTeamUpgrades();
            if (upgrades == null) continue;
            int level = upgrades.getUpgradeLevel(Upgrade.DRAGONBUFF);
            if (level < 1 || team.getTeamSpawn() == null) continue;

            Vector start = bounds.clamp(team.getTeamSpawn().toVector().add(new Vector(0, Properties.DragonSpawnHeight, 0)));
            EnderDragon dragon = (EnderDragon) world.spawnEntity(
                    new Location(world, start.getX(), start.getY(), start.getZ()), EntityType.ENDER_DRAGON);
            dragon.setInvulnerable(true);
            dragon.setPersistent(false);
            dragon.setCustomName(team.getColor() + team.getName() + " Dragon");
            dragon.setCustomNameVisible(true);
            String key = arena.getArenaName() + ":" + team.getName();
            tag(dragon, key);
            DragonBrain brain = new DragonBrain(start, bounds, settings.fireballIntervalTicks(level));
            dragons.put(team, new TeamDragon(team, dragon, brain, key));
            anySpawned = true;
        }
        if (anySpawned && task == null) {
            task = NewBedwars.plugin.getServer().getScheduler().runTaskTimer(NewBedwars.plugin, this::tick, 1L, 1L);
        }
        return anySpawned;
    }

    public void removeTeam(Team team) {
        TeamDragon dragon = dragons.remove(team);
        if (dragon != null) dragon.remove();
        if (dragons.isEmpty()) cancelTask();
    }

    public void removeAll() {
        cancelTask();
        for (TeamDragon dragon : dragons.values()) dragon.remove();
        dragons.clear();
        lastPositions.clear();
        bounds = null;
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        World world = arena.getArenaWorld();
        if (world == null || arena.status() != GameStatus.playing) {
            removeAll();
            return;
        }
        for (Team team : new ArrayList<>(dragons.keySet())) {
            TeamDragon dragon = dragons.get(team);
            if (!dragon.isValid() || !team.isAlive() || !hasOnlinePlayer(team, world)) removeTeam(team);
        }
        if (dragons.isEmpty()) return;

        Map<Team, List<Target>> playersByTeam = new HashMap<>();
        Map<UUID, Vector> seen = new HashMap<>();
        for (Team team : arena.getTeams().values()) {
            if (!team.isAlive()) continue;
            List<Target> targets = new ArrayList<>();
            for (Player p : team.getPlayers()) {
                if (!isTargetable(p, world)) continue;
                Vector pos = p.getLocation().toVector();
                Vector last = lastPositions.get(p.getUniqueId());
                Vector velocity = last == null ? new Vector() : pos.clone().subtract(last);
                seen.put(p.getUniqueId(), pos);
                if (bounds.holdsTarget(pos)) targets.add(new Target(p.getUniqueId(), Kind.PLAYER, pos, velocity));
            }
            playersByTeam.put(team, targets);
        }
        lastPositions.clear();
        lastPositions.putAll(seen);

        List<Vector> aliveBeds = new ArrayList<>();
        Map<Team, Target> beds = new HashMap<>();
        for (Team team : arena.getTeams().values()) {
            TeamBed bed = team.getTeamBed();
            if (bed == null || !bed.isAlive() || bed.getLocation() == null) continue;
            Vector pos = bed.getLocation().toVector().add(new Vector(0.5, 0.5, 0.5));
            aliveBeds.add(pos);
            if (team.isAlive()) beds.put(team, new Target(team.getName() + "#bed", Kind.BED, pos));
        }

        DragonSettings settings = Properties.Dragon;
        for (TeamDragon dragon : new ArrayList<>(dragons.values())) {
            Team owner = dragon.team();
            List<Target> enemies = new ArrayList<>();
            List<Target> enemyBeds = new ArrayList<>();
            List<Target> enemyDragons = new ArrayList<>();
            playersByTeam.forEach((team, targets) -> { if (team != owner) enemies.addAll(targets); });
            beds.forEach((team, bed) -> { if (team != owner) enemyBeds.add(bed); });
            for (TeamDragon other : dragons.values()) {
                if (other.team() != owner) {
                    enemyDragons.add(new Target(other.team().getName() + "#dragon", Kind.DRAGON, other.position()));
                }
            }
            dragon.tick(world, bounds, new Candidates(enemies, enemyBeds, enemyDragons, aliveBeds), settings);
        }
    }

    private boolean isTargetable(Player p, World world) {
        return p.isOnline() && world.equals(p.getWorld()) && !arena.getSpectators().contains(p)
                && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE);
    }

    private static boolean hasOnlinePlayer(Team team, World world) {
        for (Player p : team.getPlayers()) {
            if (p.isOnline() && world.equals(p.getWorld())) return true;
        }
        return false;
    }

    private List<Vector> groundPoints() {
        List<Vector> points = new ArrayList<>();
        for (Team team : arena.getTeams().values()) {
            addPoint(points, team.getTeamSpawn());
            if (team.getTeamBed() != null) addPoint(points, team.getTeamBed().getLocation());
            if (team.getGenerator() != null) addPoint(points, team.getGenerator().getLocation());
        }
        for (Generator generator : arena.getGenerators()) addPoint(points, generator.getLocation());
        return points;
    }

    private List<Vector> basePoints() {
        List<Vector> points = new ArrayList<>();
        for (Team team : arena.getTeams().values()) {
            addPoint(points, team.getTeamBasePos1());
            addPoint(points, team.getTeamBasePos2());
        }
        return points;
    }

    private static void addPoint(List<Vector> points, Location location) {
        if (location != null) points.add(location.toVector());
    }

    static void tag(Entity entity, String key) {
        entity.getPersistentDataContainer().set(key(), PersistentDataType.STRING, key);
    }

    public static String ownerOf(Entity entity) {
        Entity source = entity;
        if (source instanceof ComplexEntityPart part) source = part.getParent();
        String direct = source.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        if (direct != null) return direct;
        Object shooter = null;
        if (source instanceof Projectile projectile) shooter = projectile.getShooter();
        else if (source instanceof AreaEffectCloud cloud) shooter = cloud.getSource();
        if (shooter instanceof EnderDragon dragon) {
            return dragon.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        }
        return null;
    }

    private static NamespacedKey key() {
        if (ownerKey == null) ownerKey = new NamespacedKey(NewBedwars.plugin, "dragon_team");
        return ownerKey;
    }
}
