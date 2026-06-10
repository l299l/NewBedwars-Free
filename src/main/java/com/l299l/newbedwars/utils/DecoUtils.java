package com.l299l.newbedwars.utils;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class DecoUtils {
    private static final List<ArmorStand> armorStands = new ArrayList<>();

    public static ArmorStand summonSetupArmorStand(Location location, String text) {
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
        armorStands.add(armorStand);
        return armorStand;
    }

    /**
     * Removes all tracked setup armor stands within ±1 block (X/Z) and ±2 blocks (Y) of
     * the given location. Call this before calling summonSetupArmorStand for the same slot
     * to avoid leaving a ghost stand behind when a position is overridden.
     */
    public static void removeArmorStandAt(Location location) {
        armorStands.removeIf(as -> {
            if (as.getLocation().getWorld() != location.getWorld()) return false;
            if (Math.abs(as.getLocation().getBlockX() - location.getBlockX()) <= 1
                    && Math.abs(as.getLocation().getBlockY() - location.getBlockY()) <= 2
                    && Math.abs(as.getLocation().getBlockZ() - location.getBlockZ()) <= 1) {
                as.remove();
                return true;
            }
            return false;
        });
    }

    public static void summonAllArmorStands(IArena arena) {
        arena.getTeams().forEach((s, team) -> {
            summonSetupArmorStand(team.getTeamSpawn(), team.getColor() + "Spawn");
            summonSetupArmorStand(team.getTeamBed().getLocation(), team.getColor() + "Bed");
            summonSetupArmorStand(team.getTeamShop().getLocation(), team.getColor() + "Shop");
            summonSetupArmorStand(team.getTeamUpgrades().getLocation(), team.getColor() + "Upgrade");
            summonSetupArmorStand(team.getGenerator().getLocation(), team.getColor() + "Generator");
        });
        arena.getGenerators().forEach((generator) -> {
            summonSetupArmorStand(generator.getLocation(), getGeneratorColor(generator.getType()) + "Generator");
        });
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

    public static Color getColorFromChatColor(ChatColor color) {
        return switch (color) {
            case AQUA, DARK_AQUA -> Color.AQUA;
            case BLACK -> Color.BLACK;
            case BLUE, DARK_BLUE -> Color.BLUE;
            case DARK_GRAY, GRAY -> Color.GRAY;
            case DARK_GREEN, GREEN -> Color.GREEN;
            case DARK_PURPLE, LIGHT_PURPLE -> Color.PURPLE;
            case DARK_RED, RED -> Color.RED;
            case GOLD, YELLOW -> Color.YELLOW;
            default -> Color.WHITE;
        };
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
