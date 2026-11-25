package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.player.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NewBedwarsExpansion extends PlaceholderExpansion {

    private final NewBedwars plugin;

    public NewBedwarsExpansion(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "newbedwars"; }

    @Override
    public @NotNull String getAuthor() { return "l299l"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        try {
            return resolve(player, params);
        } catch (RuntimeException e) {
            if (org.bukkit.Bukkit.isPrimaryThread()) throw e;
            return "";
        }
    }

    private @Nullable String resolve(Player player, @NotNull String params) {
        if (params.startsWith("arena_")) {
            return resolveArenaParam(params.substring("arena_".length()));
        }
        if (params.startsWith("group_")) {
            return GroupPlaceholders.resolve(params.substring("group_".length()));
        }
        String global = resolveGlobal(params);
        if (global != null) return global;
        if (params.startsWith("top_")) {
            return LeaderboardPlaceholders.resolve(params.substring("top_".length()),
                    plugin.getMessages().getMsgToConsole("LeaderboardPlaceholderEmpty"));
        }

        if (player == null) return "";

        if (params.startsWith("stat_")) {
            return resolveStatParam(player, params.substring("stat_".length()));
        }

        IArena arena = ArenaRegistry.byWorld(player.getWorld());

        switch (params) {
            case "arena":
                return arena != null ? arena.getArenaName() : "";
            case "group":
                return GroupPlaceholders.groupOf(arena);
            case "status":
                return arena != null ? arena.status().name().toLowerCase() : "lobby";
            case "team": {
                if (arena == null) return "";
                Team team = arena.getTeam(player);
                return team != null ? team.getDisplayName() : "";
            }
            case "picked_team": {
                if (arena == null || com.l299l.newbedwars.arena.team.TeamSelectorManager.get() == null) return "";
                String picked = com.l299l.newbedwars.arena.team.TeamSelectorManager.get().pickedTeam(player, arena);
                if (picked == null) return "";
                Team pickedTeam = arena.getTeams().get(picked);
                return pickedTeam != null ? pickedTeam.colored(pickedTeam.getDisplayName()) : "";
            }
            case "team_color": {
                if (arena == null) return "";
                Team team = arena.getTeam(player);
                return team != null ? team.getColor().toString() : "";
            }
            case "kills": {
                if (arena == null) return "0";
                GamePlayer gp = arena.getPlayer(player.getUniqueId());
                return gp != null ? String.valueOf(gp.getKills()) : "0";
            }
            case "deaths": {
                if (arena == null) return "0";
                GamePlayer gp = arena.getPlayer(player.getUniqueId());
                return gp != null ? String.valueOf(gp.getDeaths()) : "0";
            }
            case "final_kills": {
                if (arena == null) return "0";
                GamePlayer gp = arena.getPlayer(player.getUniqueId());
                return gp != null ? String.valueOf(gp.getFinalKills()) : "0";
            }
            case "beds_broken": {
                if (arena == null) return "0";
                GamePlayer gp = arena.getPlayer(player.getUniqueId());
                return gp != null ? String.valueOf(gp.getBedsBroken()) : "0";
            }
            case "map":
                return arena != null ? arena.getArenaName() : "";
            case "in_arena":
                return String.valueOf(arena != null && (arena.isPlayerInArena(player) || arena.getSpectators().contains(player)));
            case "team_colored": {
                Team team = arena == null ? null : arena.getTeam(player);
                return team != null ? team.colored(team.getDisplayName()) : "";
            }
            case "team_short": {
                Team team = arena == null ? null : arena.getTeam(player);
                return team == null || team.getDisplayName().isEmpty() ? "" : team.getDisplayName().substring(0, 1).toUpperCase();
            }
            case "team_players": {
                Team team = arena == null ? null : arena.getTeam(player);
                return team != null ? String.valueOf(team.getPlayers().size()) : "0";
            }
            case "team_alive": {
                Team team = arena == null ? null : arena.getTeam(player);
                return String.valueOf(team != null && team.isAlive());
            }
            case "team_bed": {
                Team team = arena == null ? null : arena.getTeam(player);
                if (team == null) return "";
                return team.isBedDestroyed() ? "destroyed" : "alive";
            }
            case "teams_total":
                return arena != null ? String.valueOf(arena.getTeams().size()) : "0";
            case "teams_alive": {
                if (arena == null) return "0";
                int alive = 0;
                for (Team t : arena.getTeams().values()) if (t.isAlive()) alive++;
                return String.valueOf(alive);
            }
            case "min_players":
                return arena != null ? String.valueOf(arena.getMinPlayers()) : "0";
            case "spectators":
                return arena != null ? String.valueOf(arena.getSpectators().size()) : "0";
            case "next_phase":
                return arena != null ? arena.getNextGamePhase() : "";
            case "phase_time":
                return arena != null ? String.valueOf(arena.getPhaseTime()) : "0";
            case "phase_time_formatted":
                return arena != null ? com.l299l.newbedwars.utils.TimeUtils.formatTime(arena.getPhaseTime()) : "";
            case "game_time_formatted":
                return arena != null ? com.l299l.newbedwars.utils.TimeUtils.formatTime(arena.getGameTime()) : "";
            case "tablist_name":
                return tablistPart(player, 2);
            case "tablist_header":
                return tablistPart(player, 0);
            case "tablist_footer":
                return tablistPart(player, 1);
            case "players":
                return arena != null ? String.valueOf(arena.getActivePlayerCount()) : "0";
            case "max_players":
                return arena != null ? String.valueOf(arena.getMaxPlayers()) : "0";
            case "is_spectator":
                return arena != null ? String.valueOf(arena.getSpectators().contains(player)) : "false";
            case "phase":
                return arena != null ? arena.getCurrentGamePhase() : "";
            case "game_time": {
                if (arena == null) return "0";
                Integer t = arena.getGameTime();
                return t != null ? String.valueOf(t) : "0";
            }
            default:
                return null;
        }
    }


    private String tablistPart(Player player, int index) {
        com.l299l.newbedwars.tablist.TablistManager manager = plugin.getTablistManager();
        if (manager == null) return "";
        String[] parts = manager.preview(player);
        return parts == null || parts[index] == null ? "" : parts[index];
    }

    private @Nullable String resolveGlobal(String params) {
        switch (params) {
            case "server_name":
                return com.l299l.newbedwars.config.properties.Properties.ServerName == null ? ""
                        : org.bukkit.ChatColor.translateAlternateColorCodes('&', com.l299l.newbedwars.config.properties.Properties.ServerName);
            case "lobby_players": {
                int count = 0;
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) if (ArenaRegistry.byWorld(p.getWorld()) == null) count++;
                return String.valueOf(count);
            }
            case "total_players": {
                int count = 0;
                for (IArena a : ArenaRegistry.all()) if (a.isEnabled()) count += a.getActivePlayerCount();
                return String.valueOf(count);
            }
            case "total_arenas": {
                int count = 0;
                for (IArena a : ArenaRegistry.all()) if (a.isEnabled()) count++;
                return String.valueOf(count);
            }
            case "arenas_waiting":
                return String.valueOf(countArenas(com.l299l.newbedwars.arena.GameStatus.waiting, com.l299l.newbedwars.arena.GameStatus.starting));
            case "arenas_playing":
                return String.valueOf(countArenas(com.l299l.newbedwars.arena.GameStatus.playing, com.l299l.newbedwars.arena.GameStatus.ending));
            default:
                return null;
        }
    }

    private int countArenas(com.l299l.newbedwars.arena.GameStatus... statuses) {
        int count = 0;
        for (IArena a : ArenaRegistry.all()) {
            if (!a.isEnabled()) continue;
            for (com.l299l.newbedwars.arena.GameStatus s : statuses) if (a.status() == s) count++;
        }
        return count;
    }

    private @Nullable String resolveArenaParam(String rest) {
        String[] knownSuffixes = {"_max_players", "_min_players", "_players", "_spectators", "_status", "_phase", "_group", "_enabled"};
        for (String suffix : knownSuffixes) {
            if (rest.endsWith(suffix)) {
                String arenaName = rest.substring(0, rest.length() - suffix.length());
                if (arenaName.isEmpty()) return null;
                IArena arena = ArenaRegistry.byName(arenaName);
                if (arena == null) return "";
                return switch (suffix) {
                    case "_status"      -> arena.status().name().toLowerCase();
                    case "_players"     -> String.valueOf(arena.getActivePlayerCount());
                    case "_max_players" -> String.valueOf(arena.getMaxPlayers());
                    case "_min_players" -> String.valueOf(arena.getMinPlayers());
                    case "_spectators"  -> String.valueOf(arena.getSpectators().size());
                    case "_group"       -> GroupPlaceholders.groupOf(arena);
                    case "_enabled"     -> String.valueOf(arena.isEnabled());
                    case "_phase"       -> arena.getCurrentGamePhase();
                    default             -> null;
                };
            }
        }
        return null;
    }

    private @Nullable String resolveStatParam(Player player, String field) {
        PlayerStats stats = plugin.getPlayerManager().getStats(player.getName());
        if (stats == null) stats = PlayerStats.empty();
        return switch (field) {
            case "wins"        -> String.valueOf(stats.wins());
            case "losses"      -> String.valueOf(stats.losses());
            case "kills"       -> String.valueOf(stats.kills());
            case "deaths"      -> String.valueOf(stats.deaths());
            case "final_kills" -> String.valueOf(stats.finalKills());
            case "beds"        -> String.valueOf(stats.bedsBroken());
            case "games"       -> String.valueOf(stats.gamesPlayed());
            case "kd"          -> ratio(stats.kills(), stats.deaths());
            case "wl"          -> ratio(stats.wins(), stats.losses());
            default            -> null;
        };
    }

    private String ratio(int a, int b) {
        if (b == 0) return a == 0 ? "0.0" : String.valueOf((double) a);
        return String.format("%.1f", (double) a / b);
    }
}
