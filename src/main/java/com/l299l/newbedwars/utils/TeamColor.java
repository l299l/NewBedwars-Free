package com.l299l.newbedwars.utils;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * The one place that maps a team's {@link ChatColor} to dyed blocks, leather and firework colors, so every
 * feature agrees on what a color looks like. Block materials are resolved by name, which keeps this working
 * on every supported version.
 */
public final class TeamColor {
    private TeamColor() {}

    /** Dyed block families that exist in all 16 colors. */
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

    /** The dye color of a team color. {@code null} gives gray, a non-color format gives white. */
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

    /** Color for leather armor. */
    public static Color leather(ChatColor color) {
        return dye(color).getColor();
    }

    /** Color for fireworks. */
    public static Color firework(ChatColor color) {
        return dye(color).getFireworkColor();
    }

    /** The family of a white (undyed-looking) block such as {@code WHITE_WOOL} (plain {@code TERRACOTTA} counts as white), or {@code null} for anything else. */
    public static Kind undyedKind(Material material) {
        if (material == null) return null;
        if (material == Material.TERRACOTTA) return Kind.TERRACOTTA;
        String name = material.name();
        for (Kind kind : Kind.values()) {
            if (name.equals("WHITE_" + kind.suffix)) return kind;
        }
        return null;
    }

    /** The family of a dyed block in any color (e.g. {@code RED_CONCRETE}), or {@code null} for anything else. */
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
                // not a dye prefix, e.g. RED_GLAZED_TERRACOTTA
            }
        }
        return null;
    }

    /** Turns a white block (wool, terracotta, glass, concrete, carpet, bed, ...) into the team's color; anything else is returned as is. */
    public static Material recolor(Material material, ChatColor color) {
        Kind kind = undyedKind(material);
        return kind == null ? material : block(color, kind);
    }

    /**
     * Parses a team color name: dye names (BLUE, LIME, PINK, LIGHT_GRAY, ...) first, then plain
     * {@link ChatColor} names. Throws {@link IllegalArgumentException} for unknown names.
     */
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
