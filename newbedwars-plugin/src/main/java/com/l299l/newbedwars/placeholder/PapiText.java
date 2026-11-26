package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.text.HexColors;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

public final class PapiText {
    private static volatile BiFunction<Player, String, String> resolver;

    private PapiText() {
    }

    public static void enable() {
        resolver = PapiBridge::apply;
    }

    public static void disable() {
        resolver = null;
    }

    static void use(BiFunction<Player, String, String> custom) {
        resolver = custom;
    }

    public static boolean isEnabled() {
        return resolver != null;
    }

    public static String apply(Player player, String raw) {
        if (player == null) return HexColors.translate(raw);
        return resolve(player, raw);
    }

    public static String applyServerWide(String raw) {
        return resolve(null, raw);
    }

    private static String resolve(Player player, String raw) {
        String text = HexColors.translate(raw);
        BiFunction<Player, String, String> current = resolver;
        if (current == null || text == null || text.indexOf('%') < 0) return text;
        try {
            String out = current.apply(player, text);
            return out == null ? text : ChatColor.translateAlternateColorCodes('&', HexColors.translate(out));
        } catch (RuntimeException | LinkageError e) {
            return text;
        }
    }
}
