package com.l299l.newbedwars.arena.setup;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;

import java.util.ArrayList;
import java.util.List;

public class DecoUtils {

    public static final String SETUP_MARKER_TAG = "nbw_setup_marker";
    private static final List<ArmorStand> armorStands = new ArrayList<>();

    public static ArmorStand summonSetupArmorStand(Location location, String text) {
        if (location == null || location.getWorld() == null) return null;
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setCollidable(false);
        armorStand.setCanPickupItems(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setMarker(true);

        armorStand.setPersistent(false);
        armorStand.addScoreboardTag(SETUP_MARKER_TAG);
        armorStands.add(armorStand);
        return armorStand;
    }

    public static void removeArmorStandAt(Location location, String name) {
        armorStands.removeIf(as -> {
            if (!isNear(as.getLocation(), location) || !name.equals(as.getCustomName())) return false;
            as.remove();
            return true;
        });
    }

    static boolean isNear(Location marker, Location location) {
        return marker.getWorld() == location.getWorld()
                && Math.abs(marker.getBlockX() - location.getBlockX()) <= 1
                && Math.abs(marker.getBlockY() - location.getBlockY()) <= 2
                && Math.abs(marker.getBlockZ() - location.getBlockZ()) <= 1;
    }

    public static void summonAllArmorStands(IArena arena) {
        arena.getTeams().forEach((s, team) -> {
            summonSetupArmorStand(team.getTeamSpawn(), team.getColor() + "Spawn");
            if (team.getTeamBed() != null) summonSetupArmorStand(team.getTeamBed().getLocation(), team.getColor() + "Bed");
            if (team.getTeamShop() != null) summonSetupArmorStand(team.getTeamShop().getLocation(), team.getColor() + "Shop");
            if (team.getTeamUpgrades() != null) summonSetupArmorStand(team.getTeamUpgrades().getLocation(), team.getColor() + "Upgrades");
            if (team.getGenerator() != null) summonSetupArmorStand(team.getGenerator().getLocation(), team.getColor() + "Generator");
        });
        arena.getGenerators().forEach((generator) -> {
            summonSetupArmorStand(generator.getLocation(), getGeneratorColor(generator.getType()) + "Generator");
        });
    }

    @SuppressWarnings("deprecation")
    public static void makeAdult(LivingEntity entity) {
        if (entity instanceof Zombie zombie) {
            zombie.setBaby(false);
        } else if (entity instanceof Ageable ageable) {
            ageable.setAdult();
            ageable.setAgeLock(true);
        }
    }

    public static void removeArmorStand(ArmorStand armorStand) {
        armorStand.remove();
        armorStands.remove(armorStand);
    }

    public static void removeAllArmorStands(IArena arena) {
        armorStands.removeIf(as -> {
            if (as.getLocation().getWorld() == arena.getArenaWorld()) {
                as.remove();
                return true;
            }
            return false;
        });
        World arenaWorld = arena.getArenaWorld();
        if (arenaWorld != null) {
            arenaWorld.getEntitiesByClass(ArmorStand.class).stream()
                    .filter(as -> !as.isVisible() && as.isMarker())
                    .forEach(as -> {
                        as.remove();
                        armorStands.remove(as);
                    });
        }
    }

    public static boolean isSetupMarker(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof ArmorStand as)) return false;
        return as.getScoreboardTags().contains(SETUP_MARKER_TAG) || (!as.isVisible() && as.isMarker());
    }

    private static ChatColor getGeneratorColor(GeneratorType generatorType) {
        return switch (generatorType) {
            case BASIC -> ChatColor.GOLD;
            case DIAMOND -> ChatColor.AQUA;
            case EMERALD -> ChatColor.GREEN;
            default -> ChatColor.WHITE;
        };
    }
}
