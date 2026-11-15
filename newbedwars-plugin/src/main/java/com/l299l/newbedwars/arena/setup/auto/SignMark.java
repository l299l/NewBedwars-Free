package com.l299l.newbedwars.arena.setup.auto;

import org.bukkit.ChatColor;

import java.util.Locale;

public record SignMark(Pos pos, Kind kind, String team, String extra) {

    public enum Kind {
        SPAWN("[spawn]"),
        SHOP("[shop]"),
        UPGRADES("[upgrades]", "[upgrade]"),
        GENERATOR("[gen]", "[generator]");

        private final String[] tags;

        Kind(String... tags) {
            this.tags = tags;
        }

        static Kind byTag(String text) {
            for (Kind kind : values()) {
                for (String tag : kind.tags) {
                    if (tag.equals(text)) return kind;
                }
            }
            return null;
        }
    }

    public static SignMark parse(Pos pos, String[] lines) {
        if (lines == null || lines.length == 0 || lines[0] == null) return null;
        Kind kind = Kind.byTag(clean(lines[0]).toLowerCase(Locale.ROOT));
        if (kind == null) return null;
        return new SignMark(pos, kind, line(lines, 1), line(lines, 2));
    }

    private static String line(String[] lines, int index) {
        return lines.length > index && lines[index] != null ? clean(lines[index]) : "";
    }

    private static String clean(String text) {
        String stripped = ChatColor.stripColor(text);
        return (stripped == null ? text : stripped).trim();
    }
}
