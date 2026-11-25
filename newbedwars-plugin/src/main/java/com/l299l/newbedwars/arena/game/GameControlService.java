package com.l299l.newbedwars.arena.game;

import com.l299l.newbedwars.api.maintenance.MaintenanceResult.Code;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.world.template.ArenaResetPipeline;
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
                if (!canForceStop(arena, status)) return outcome(Code.BUSY, "ArenaBusy", name, Map.of("/arenaname/", name));
                arena.broadcast("AdminStoppedGame", admin);
                arena.stop();
                return outcome(Code.OK, "AdminStoppedGame", name, admin);
            }
            default -> {
                if (status != GameStatus.playing) return outcome(Code.NOT_READY, "GameNotInProgress", name, Map.of());
                String next = arena.getNextGamePhase();
                if (next == null || next.isEmpty()) return outcome(Code.ALREADY_DONE, "AlreadyLastPhase", name, Map.of());
                arena.advancePhase();
                return outcome(Code.OK, "PhaseAdvancedTo", name, Map.of("/phase/", next));
            }
        }
    }

    private static boolean canForceStop(IArena arena, GameStatus status) {
        if (!arena.isEnabled() || status == null || status == GameStatus.restarting) return false;
        String name = arena.getArenaName();
        return !ArenaMaintenanceService.inFlight(name) && !ArenaResetPipeline.isCopying(name);
    }

    private static Outcome teamAction(String arenaName, String teamName, java.util.function.BiFunction<IArena, Team, Outcome> action) {
        IArena arena = arenaName == null ? null : ArenaRegistry.find(arenaName);
        if (arena == null) return outcome(Code.NOT_FOUND, "ArenaNotExists", arenaName, Map.of("/arenaname/", String.valueOf(arenaName)));
        if (arena.status() != GameStatus.playing) return outcome(Code.NOT_READY, "GameNotInProgress", arena.getArenaName(), Map.of());
        Team team = teamName == null ? null : arena.getTeam(teamName);
        if (team == null) {
            for (Team candidate : arena.getTeams().values()) {
                if (candidate.getName().equalsIgnoreCase(String.valueOf(teamName))) team = candidate;
            }
        }
        if (team == null) return outcome(Code.NOT_FOUND, "TeamNotFound", arena.getArenaName(), Map.of());
        return action.apply(arena, team);
    }

    private static Map<String, String> teamValues(Team team, String actor) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("/admin/", actor == null ? "" : actor);
        values.put("/team/", team.colored(team.getDisplayName()));
        return values;
    }

    public static Outcome destroyBed(String arenaName, String teamName, String actor) {
        return teamAction(arenaName, teamName, (arena, team) -> {
            if (team.getTeamBed() == null || team.isBedDestroyed()) {
                return outcome(Code.ALREADY_DONE, "AdminBedAlreadyDestroyed", arena.getArenaName(), teamValues(team, actor));
            }
            if (!arena.destroyBed(team)) return outcome(Code.FAILED, "AdminActionCancelled", arena.getArenaName(), Map.of());
            return outcome(Code.OK, "AdminDestroyedBed", arena.getArenaName(), teamValues(team, actor));
        });
    }

    public static Outcome eliminateTeam(String arenaName, String teamName, String actor) {
        return teamAction(arenaName, teamName, (arena, team) -> {
            if (!team.isAlive()) return outcome(Code.ALREADY_DONE, "AdminTeamAlreadyOut", arena.getArenaName(), teamValues(team, actor));
            if (!arena.eliminateTeam(team)) return outcome(Code.FAILED, "AdminActionCancelled", arena.getArenaName(), Map.of());
            return outcome(Code.OK, "AdminEliminatedTeam", arena.getArenaName(), teamValues(team, actor));
        });
    }

    public static Outcome setUpgradeLevel(String arenaName, String teamName, String upgradeName, int level, String actor) {
        return teamAction(arenaName, teamName, (arena, team) -> {
            Map<String, String> values = teamValues(team, actor);
            values.put("/upgrade/", String.valueOf(upgradeName));
            values.put("/level/", String.valueOf(level));
            com.l299l.newbedwars.arena.shops.Upgrade upgrade = null;
            try {
                if (upgradeName != null) upgrade = com.l299l.newbedwars.arena.shops.Upgrade.valueOf(upgradeName);
            } catch (IllegalArgumentException ignored) {
            }
            if (upgrade == null || team.getTeamUpgrades() == null) {
                return outcome(Code.NOT_FOUND, "AdminUpgradeNotFound", arena.getArenaName(), values);
            }
            values.put("/upgrade/", upgrade.name());
            int max = upgrade.maxLevel;
            values.put("/max/", String.valueOf(max));
            if (level < 0 || level > max) return outcome(Code.INVALID_NAME, "AdminUpgradeLevelInvalid", arena.getArenaName(), values);
            if (!team.getTeamUpgrades().setLevel(upgrade, level)) {
                return outcome(Code.INVALID_NAME, "AdminUpgradeCannotLower", arena.getArenaName(), values);
            }
            return outcome(Code.OK, "AdminSetUpgrade", arena.getArenaName(), values);
        });
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
