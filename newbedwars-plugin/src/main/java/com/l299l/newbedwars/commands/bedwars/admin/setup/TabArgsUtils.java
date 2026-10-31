package com.l299l.newbedwars.commands.bedwars.admin.setup;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TabArgsUtils {

    public static List<String> getCommandNums(String[] args) {
        if(args.length == 2) {
            List<String> arguments = new ArrayList<>();
            for (int i = 2; i < 17; i++) {
                arguments.add(String.valueOf(i));
            }
            return arguments;
        }
        return null;
    }

    /** Team names at {@code args.length == start}, then the player's block coordinates. */
    public static List<String> getTabCordsWithTeam(Player player, String[] args, int start) {
        if (args.length == start) return teamNames(player);
        return coords(player, args.length - start - 1);
    }

    /** The player's block coordinates, the x one at {@code args.length == start}. */
    public static List<String> getTabCords(Player player, String[] args, int start) {
        return coords(player, args.length - start);
    }

    /** Team names, then the shop/upgrade entity types, then the player's block coordinates. */
    public static List<String> getTabEntities(Player player, String[] args) {
        if (args.length == 2) return teamNames(player);
        if (args.length == 3) {
            List<EntityType> wrong = wrongEntities();
            List<String> names = new ArrayList<>();
            for (EntityType value : EntityType.values()) {
                if (!wrong.contains(value)) names.add(value.name());
            }
            return names;
        }
        return coords(player, args.length - 4);
    }

    private static List<String> teamNames(Player player) {
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        return arena == null ? new ArrayList<>() : new ArrayList<>(arena.getTeams().keySet());
    }

    /** "x y z", "y z" or "z" of the player's block position for coordinate {@code index} 0, 1 or 2; otherwise {@code null}. */
    private static List<String> coords(Player player, int index) {
        if (index < 0 || index > 2) return null;
        int[] xyz = {player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()};
        StringBuilder sb = new StringBuilder();
        for (int i = index; i < 3; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(xyz[i]);
        }
        return Collections.singletonList(sb.toString());
    }

    private static final Set<String> EXCLUDED_LIVING = Set.of(
        "PLAYER",
        "SHULKER",
        "ALLAY", "VEX", "GHAST", "BLAZE", "WITHER", "PHANTOM",
        "ZOGLIN", "WARDEN", "BEE",
        "COD", "SALMON", "PUFFERFISH", "TROPICAL_FISH",
        "SQUID", "GLOW_SQUID", "DOLPHIN", "TURTLE", "AXOLOTL",
        "ELDER_GUARDIAN", "GUARDIAN"
    );

    public static ArrayList<EntityType> wrongEntities() {
        ArrayList<EntityType> wrongEntity = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            Class<?> clazz = type.getEntityClass();
            if (clazz == null || !LivingEntity.class.isAssignableFrom(clazz)) {
                wrongEntity.add(type);
                continue;
            }
            if (EXCLUDED_LIVING.contains(type.name())) {
                wrongEntity.add(type);
            }
        }
        return wrongEntity;
    }
}
