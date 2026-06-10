package com.l299l.newbedwars.utils;

import com.l299l.newbedwars.arena.Arena;
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

    public static List<String> getTabCordsWithTeam(Player player, String[] args, int start) {
        if(args.length == start) {
            IArena arena = Arena.arenaByWorld.get(player.getWorld());
            return new ArrayList<>(arena.getTeams().keySet());
        }else if(args.length >= start + 1) {
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            if (args.length == start + 1) {
                String string = x + " " + y + " " + z;
                return Collections.singletonList(string);
            } else if (args.length == start + 2) {
                String string = y + " " + z;
                return Collections.singletonList(string);
            } else if (args.length == start + 3) {
                String string = Integer.toString(z);
                return Collections.singletonList(string);
            }
        }
        return null;
    }

    public static List<String> getTabCords(Player player, String[] args, int start) {
        if(args.length == start) {
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            String string = x + " " + y + " " + z;
            return Collections.singletonList(string);
        }else if(args.length == start + 1){
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            String string = y + " " + z;
            return Collections.singletonList(string);
        }else if(args.length == start + 2){
            int z = player.getLocation().getBlockZ();
            String string = Integer.toString(z);
            return Collections.singletonList(string);
        }
        return null;
    }

    public static List<String> getTabEntities(Player player, String[] args) {
        if(args.length == 2) {
            IArena arena = Arena.arenaByWorld.get(player.getWorld());
            return new ArrayList<>(arena.getTeams().keySet());
        }else if(args.length == 3) {
            EntityType[] values = EntityType.values();
            ArrayList<String> arrayList = new ArrayList<>();
            for (EntityType value : values) {
                if (!(TabArgsUtils.wrongEntities().contains(value))) {
                    arrayList.add(value.name());
                }
            }
            return arrayList;
        }else if(args.length >= 4) {
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            if (args.length == 4) {
                String string = x + " " + y + " " + z;
                return Collections.singletonList(string);
            } else if (args.length == 5) {
                String string = y + " " + z;
                return Collections.singletonList(string);
            } else if (args.length == 6) {
                String string = Integer.toString(z);
                return Collections.singletonList(string);
            }
        }
        return null;
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
