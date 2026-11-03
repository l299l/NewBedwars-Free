package com.l299l.newbedwars.utils;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;

public final class TeamColor {
    private TeamColor() {}

    public enum Kind {
        WOOL("WOOL"),
        TERRACOTTA("TERRACOTTA"),
        GLASS("STAINED_GLASS"),
        GLASS_PANE("STAINED_GLASS_PANE"),
        CONCRETE("CONCRETE"),
        CONCRETE_POWDER("CONCRETE_POWDER"),
        CARPET("CARPET"),
        BED("BED");

        private final String suffix;

        Kind(String suffix) {
            this.suffix = suffix;
        }
    }

    public static DyeColor dye(ChatColor color) {
        if (color == null) return DyeColor.GRAY;
        return switch (color) {
            case WHITE -> DyeColor.WHITE;
            case GRAY -> DyeColor.LIGHT_GRAY;
            case DARK_GRAY -> DyeColor.GRAY;
            case BLACK -> DyeColor.BLACK;
            case RED, DARK_RED -> DyeColor.RED;
            case GOLD -> DyeColor.ORANGE;
            case YELLOW -> DyeColor.YELLOW;
            case GREEN -> DyeColor.LIME;
            case DARK_GREEN -> DyeColor.GREEN;
            case AQUA -> DyeColor.LIGHT_BLUE;
            case DARK_AQUA -> DyeColor.CYAN;
            case BLUE, DARK_BLUE -> DyeColor.BLUE;
            case DARK_PURPLE -> DyeColor.PURPLE;
            case LIGHT_PURPLE -> DyeColor.PINK;
            default -> DyeColor.WHITE;
        };
    }

    public static Material block(ChatColor color, Kind kind) {
        return Material.valueOf(dye(color).name() + "_" + kind.suffix);
    }

    public static Material wool(ChatColor color) {
        return block(color, Kind.WOOL);
    }

    public static Color leather(ChatColor color) {
        return dye(color).getColor();
    }

    public static Color firework(ChatColor color) {
        return dye(color).getFireworkColor();
    }

    public static Kind undyedKind(Material material) {
        if (material == null) return null;
        if (material == Material.TERRACOTTA) return Kind.TERRACOTTA;
        String name = material.name();
        for (Kind kind : Kind.values()) {
            if (name.equals("WHITE_" + kind.suffix)) return kind;
        }
        return null;
    }

    public static Kind kindOf(Material material) {
        if (material == null) return null;
        String name = material.name();
        for (Kind kind : Kind.values()) {
            String end = "_" + kind.suffix;
            if (!name.endsWith(end)) continue;
            try {
                DyeColor.valueOf(name.substring(0, name.length() - end.length()));
                return kind;
            } catch (IllegalArgumentException ignored) {

            }
        }
        return null;
    }

    public static Material recolor(Material material, ChatColor color) {
        Kind kind = undyedKind(material);
        return kind == null ? material : block(color, kind);
    }

    public static ChatColor parse(String name) {
        return switch (name.toUpperCase()) {
            case "PINK" -> ChatColor.LIGHT_PURPLE;
            case "ORANGE" -> ChatColor.GOLD;
            case "LIME" -> ChatColor.GREEN;
            case "LIGHT_GRAY" -> ChatColor.GRAY;
            case "GRAY" -> ChatColor.DARK_GRAY;
            case "CYAN" -> ChatColor.DARK_AQUA;
            case "LIGHT_BLUE" -> ChatColor.AQUA;
            case "BLUE" -> ChatColor.DARK_BLUE;
            case "PURPLE" -> ChatColor.DARK_PURPLE;
            case "GREEN" -> ChatColor.DARK_GREEN;
            default -> {
                ChatColor c = ChatColor.valueOf(name.toUpperCase());
                if (!c.isColor()) throw new IllegalArgumentException(name + " is not a color");
                yield c;
            }
        };
    }
}
