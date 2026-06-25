package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.player.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholders (prefix: %newbedwars_<param>%):
 *
 * In-game per-player:
 *   arena            — arena name the player is in, or ""
 *   status           — game status (waiting/starting/playing/ending/restarting), or "lobby"
 *   team             — team name, or ""
 *   team_color       — team ChatColor code, or ""
 *   kills            — kills in current game
 *   deaths           — deaths in current game
 *   final_kills      — final kills in current game
 *   beds_broken      — beds broken in current game
 *   players          — current player count in the arena
 *   max_players      — max players in the arena
 *   is_spectator     — true / false
 *   phase            — current phase name, or ""
 *   game_time        — seconds elapsed since game start, or 0
 *
 * Lifetime stats per-player:
 *   stat_wins        — total wins
 *   stat_losses      — total losses
 *   stat_kills       — total kills
 *   stat_deaths      — total deaths
 *   stat_final_kills — total final kills
 *   stat_beds        — total beds broken
 *   stat_games       — total games played
 *   stat_kd          — kill/death ratio (1 decimal, e.g. "2.3")
 *   stat_wl          — win/loss ratio (1 decimal)
 *
 * Per-arena (not player-specific):
 *   arena_<name>_status      — game status of the named arena
 *   arena_<name>_players     — player count
 *   arena_<name>_max_players — max players
 *   arena_<name>_phase       — current phase name
 */
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
        if (params.startsWith("arena_")) {
            return resolveArenaParam(params.substring("arena_".length()));
        }

        if (player == null) return "";

        if (params.startsWith("stat_")) {
            return resolveStatParam(player, params.substring("stat_".length()));
        }

        IArena arena = Arena.arenaByWorld.get(player.getWorld());

        switch (params) {
            case "arena":
                return arena != null ? arena.getArenaName() : "";
            case "status":
                return arena != null ? arena.status().name().toLowerCase() : "lobby";
            case "team": {
                if (arena == null) return "";
                Team team = arena.getTeam(player);
                return team != null ? team.getName() : "";
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
            case "players":
                return arena != null ? String.valueOf(arena.getPlayers().size()) : "0";
            case "max_players":
                return arena != null ? String.valueOf(arena.getTeams().size() * arena.getMaxInTeam()) : "0";
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


    private @Nullable String resolveArenaParam(String rest) {
        String[] knownSuffixes = {"_max_players", "_players", "_status", "_phase"};
        for (String suffix : knownSuffixes) {
            if (rest.endsWith(suffix)) {
                String arenaName = rest.substring(0, rest.length() - suffix.length());
                if (arenaName.isEmpty()) return null;
                IArena arena = Arena.arenaByName.get(arenaName);
                if (arena == null) return "";
                return switch (suffix) {
                    case "_status"      -> arena.status().name().toLowerCase();
                    case "_players"     -> String.valueOf(arena.getPlayers().size());
                    case "_max_players" -> String.valueOf(arena.getTeams().size() * arena.getMaxInTeam());
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
