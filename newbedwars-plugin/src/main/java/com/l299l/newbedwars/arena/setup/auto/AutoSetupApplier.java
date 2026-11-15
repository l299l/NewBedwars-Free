package com.l299l.newbedwars.arena.setup.auto;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.BasicGenerator;
import com.l299l.newbedwars.arena.generators.DiamondGenerator;
import com.l299l.newbedwars.arena.generators.EmeraldGenerator;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.admin.setup.TabArgsUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AutoSetupApplier {

    public record Outcome(int teams, int diamondGenerators, int emeraldGenerators, int signs, List<ScanResult.Warning> warnings) {}

    private AutoSetupApplier() {}

    public static Outcome apply(IArena arena, ScanResult result, AreaTracker areas) {
        World world = arena.getArenaWorld();
        List<ScanResult.Warning> warnings = new ArrayList<>(result.warnings);
        Pos center = center(result);
        int teams = 0;
        for (ScanResult.TeamPlan plan : result.teams) {
            Team team = arena.getTeams().get(plan.name);
            if (team == null) {
                arena.createTeam(plan.name, plan.color);
                team = arena.getTeams().get(plan.name);
                teams++;
            }
            applyTeam(world, team, plan, center, warnings);
        }
        int diamond = addGenerators(arena, result.diamondGenerators, GeneratorKind.DIAMOND);
        int emerald = addGenerators(arena, result.emeraldGenerators, GeneratorKind.EMERALD);
        if (result.quickVoidY != null && arena.getQuickVoidY() == null) arena.setQuickVoidY(result.quickVoidY);
        areas.refresh(arena);
        int signs = removeSigns(world, result.usedSigns);
        return new Outcome(teams, diamond, emerald, signs, warnings);
    }

    private static void applyTeam(World world, Team team, ScanResult.TeamPlan plan, Pos center, List<ScanResult.Warning> warnings) {
        ChatColor color = team.getColor();
        if (!team.isBedSet() && plan.bed != null) {
            Location bed = at(world, plan.bed);
            team.setTeamBed(bed);
            DecoUtils.summonSetupArmorStand(bed, color + "Bed");
        }
        if (!team.isSpawnSet() && plan.spawn != null) {
            Location spawn = at(world, plan.spawn);
            face(spawn, center);
            team.setTeamSpawn(spawn);
            DecoUtils.summonSetupArmorStand(spawn, color + "Spawn");
        }
        if (!team.isShopSet() && plan.shop != null) {
            Location shop = at(world, plan.shop);
            face(shop, center);
            team.setTeamShop(new TeamShop(shop, entity(plan.shopEntity, plan.name, warnings), team));
            DecoUtils.summonSetupArmorStand(shop, color + "Shop");
        }
        if (!team.isUpgradesSet() && plan.upgrades != null) {
            Location upgrades = at(world, plan.upgrades);
            face(upgrades, center);
            team.setTeamUpgrades(new TeamUpgrades(upgrades, entity(plan.upgradesEntity, plan.name, warnings), team));
            DecoUtils.summonSetupArmorStand(upgrades, color + "Upgrades");
        }
        if (!team.isGeneratorSet() && plan.generator != null) {
            Location generator = at(world, plan.generator);
            team.setGenerator(new BasicGenerator(generator));
            DecoUtils.summonSetupArmorStand(generator, color + "Generator");
        }
    }

    private enum GeneratorKind { DIAMOND, EMERALD }

    private static int addGenerators(IArena arena, List<Pos> positions, GeneratorKind kind) {
        int added = 0;
        for (Pos pos : positions) {
            Location location = at(arena.getArenaWorld(), pos);
            if (hasGeneratorNear(arena, location)) continue;
            Generator generator = kind == GeneratorKind.DIAMOND ? new DiamondGenerator(location) : new EmeraldGenerator(location);
            arena.addGenerator(generator);
            DecoUtils.summonSetupArmorStand(location, (kind == GeneratorKind.EMERALD ? ChatColor.GREEN : ChatColor.BLUE) + "Generator");
            added++;
        }
        return added;
    }

    private static boolean hasGeneratorNear(IArena arena, Location location) {
        for (Generator existing : arena.getGenerators()) {
            Location other = existing.getLocation();
            if (other != null && other.getWorld() == location.getWorld() && other.distanceSquared(location) <= 4) return true;
        }
        return false;
    }

    private static EntityType entity(String name, String team, List<ScanResult.Warning> warnings) {
        if (name == null) return EntityType.VILLAGER;
        try {
            EntityType type = EntityType.valueOf(name.toUpperCase(Locale.ROOT));
            if (!TabArgsUtils.wrongEntities().contains(type)) return type;
        } catch (IllegalArgumentException ignored) {
        }
        warnings.add(new ScanResult.Warning("AutoSetup-WarnBadEntity", name + " (" + team + ")"));
        return EntityType.VILLAGER;
    }

    private static int removeSigns(World world, List<SignMark> signs) {
        int removed = 0;
        for (SignMark sign : signs) {
            Block block = world.getBlockAt(sign.pos().x(), sign.pos().y(), sign.pos().z());
            if (!block.getType().name().endsWith("_SIGN")) continue;
            block.setType(Material.AIR);
            removed++;
        }
        return removed;
    }

    private static Pos center(ScanResult result) {
        if (result.teams.isEmpty()) return null;
        long x = 0;
        long z = 0;
        int y = 0;
        for (ScanResult.TeamPlan team : result.teams) {
            x += team.bed.x();
            z += team.bed.z();
            y = team.bed.y();
        }
        return new Pos((int) (x / result.teams.size()), y, (int) (z / result.teams.size()));
    }

    private static void face(Location location, Pos center) {
        if (center == null) return;
        double dx = center.x() + 0.5 - location.getX();
        double dz = center.z() + 0.5 - location.getZ();
        if (dx == 0 && dz == 0) return;
        location.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
    }

    private static Location at(World world, Pos pos) {
        return new Location(world, pos.x() + 0.5, pos.y(), pos.z() + 0.5);
    }
}
