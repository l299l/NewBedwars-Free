package com.l299l.newbedwars.scoreboard;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardOverrides {
    private static final Map<UUID, String[]> OVERRIDES = new ConcurrentHashMap<>();
    private static final Set<UUID> HIDDEN = ConcurrentHashMap.newKeySet();

    private ScoreboardOverrides() {
    }

    public static void set(Player player, String id, String arenaName) {
        OVERRIDES.put(player.getUniqueId(), new String[]{id, arenaName});
    }

    public static boolean clear(Player player) {
        return OVERRIDES.remove(player.getUniqueId()) != null;
    }

    public static String get(Player player, String arenaName) {
        String[] value = OVERRIDES.get(player.getUniqueId());
        if (value == null) return null;
        if (!Objects.equals(value[1], arenaName)) {
            OVERRIDES.remove(player.getUniqueId());
            return null;
        }
        return value[0];
    }

    public static void hide(Player player) {
        HIDDEN.add(player.getUniqueId());
    }

    public static boolean unhide(Player player) {
        return HIDDEN.remove(player.getUniqueId());
    }

    public static boolean isHidden(Player player) {
        return HIDDEN.contains(player.getUniqueId());
    }

    public static void forget(Player player) {
        OVERRIDES.remove(player.getUniqueId());
        HIDDEN.remove(player.getUniqueId());
    }

    public static void clearAll() {
        OVERRIDES.clear();
        HIDDEN.clear();
    }
}
