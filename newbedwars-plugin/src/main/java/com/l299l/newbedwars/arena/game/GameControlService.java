package com.l299l.newbedwars.arena.game;

import com.l299l.newbedwars.api.maintenance.MaintenanceResult.Code;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.ChatColor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GameControlService {
    public static final int MAX_BROADCAST_LENGTH = 256;

    public enum Action { START, FORCE_START, STOP, FORCE_STOP, NEXT_PHASE }

    public record Outcome(Code code, String key, String arena, Map<String, String> replacements) {
        public boolean success() {
            return code == Code.OK;
        }
    }

    private GameControlService() {}

    private static Outcome outcome(Code code, String key, String arena, Map<String, String> replacements) {
        return new Outcome(code, key, arena, replacements);
    }

    public static Outcome run(String arenaName, Action action, String actor) {
        IArena arena = arenaName == null ? null : ArenaRegistry.find(arenaName);
        if (arena == null) return outcome(Code.NOT_FOUND, "ArenaNotExists", arenaName, Map.of("/arenaname/", String.valueOf(arenaName)));
        return run(arena, action, actor);
    }

    public static Outcome run(IArena arena, Action action, String actor) {
        String name = arena.getArenaName();
        Map<String, String> admin = Map.of("/admin/", actor == null ? "" : actor);
        GameStatus status = arena.status();
        switch (action) {
            case FORCE_START -> {
                if (status != GameStatus.starting && status != GameStatus.waiting) return outcome(Code.NOT_READY, "GameNotWaiting", name, Map.of());
                arena.broadcast("AdminForcedStart", admin);
                arena.start();
                return outcome(Code.OK, "AdminForcedStart", name, admin);
            }
            case START -> {
                if (status != GameStatus.waiting) return outcome(Code.NOT_READY, "GameNotWaiting", name, Map.of());
                arena.broadcast("AdminForcedStart", admin);
                arena.setArenaStarting();
                return outcome(Code.OK, "AdminForcedStart", name, admin);
            }
            case STOP -> {
                if (status != GameStatus.playing) return outcome(Code.NOT_READY, "GameNotInProgress", name, Map.of());
                arena.endGame("DRAW");
                return outcome(Code.OK, "GameDraw", name, Map.of());
            }
            case FORCE_STOP -> {
                arena.broadcast("AdminStoppedGame", admin);
                arena.stop();
                return outcome(Code.OK, "AdminStoppedGame", name, admin);
            }
            default -> {
                if (status != GameStatus.playing) return outcome(Code.NOT_READY, "GameNotInProgress", name, Map.of());
                String next = arena.getNextGamePhase();
                if (next == null) next = "";
                arena.advancePhase();
                return outcome(Code.OK, next.isEmpty() ? "PhaseAdvanced" : "PhaseAdvancedTo", name, Map.of("/phase/", next));
            }
        }
    }

    public static Outcome broadcast(String arenaName, String actor, String message) {
        IArena arena = arenaName == null ? null : ArenaRegistry.find(arenaName);
        if (arena == null) return outcome(Code.NOT_FOUND, "ArenaNotExists", arenaName, Map.of("/arenaname/", String.valueOf(arenaName)));
        return broadcast(arena, actor, message);
    }

    public static Outcome broadcast(IArena arena, String actor, String message) {
        String text = message == null ? "" : message.trim();
        if (text.isEmpty() || text.length() > MAX_BROADCAST_LENGTH) {
            return outcome(Code.INVALID_NAME, "AdminBroadcastInvalid", arena.getArenaName(),
                    Map.of("/max/", String.valueOf(MAX_BROADCAST_LENGTH)));
        }
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("/admin/", actor == null ? "" : actor);
        replacements.put("/message/", ChatColor.translateAlternateColorCodes('&', text));
        arena.broadcast("AdminBroadcast", replacements);
        return outcome(Code.OK, "AdminBroadcast", arena.getArenaName(), replacements);
    }
}
