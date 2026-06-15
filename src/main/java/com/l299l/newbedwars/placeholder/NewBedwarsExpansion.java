package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholders:
 *
 * Per-player (resolved from the arena the player is currently in):
 *   %newbedwars_arena%           — arena name, or empty string if not in one
 *   %newbedwars_status%          — game status (waiting/starting/playing/ending/restarting), or "lobby"
 *   %newbedwars_team%            — team name, or empty string
 *   %newbedwars_kills%           — kills in current game
 *   %newbedwars_final_kills%     — final kills in current game
 *   %newbedwars_beds_broken%     — beds broken in current game
 *   %newbedwars_players%         — players currently in the arena
 *   %newbedwars_max_players%     — max player capacity of the arena
 *   %newbedwars_is_spectator%    — true / false
 *   %newbedwars_phase%           — current phase name, or empty string
 *
 * Per-arena (by name, not player-specific):
 *   %newbedwars_arena_<name>_status%       — game status of the named arena
 *   %newbedwars_arena_<name>_players%      — player count of the named arena
 *   %newbedwars_arena_<name>_max_players%  — max players of the named arena
 *   %newbedwars_arena_<name>_phase%        — current phase of the named arena
 */
public class NewBedwarsExpansion extends PlaceholderExpansion {

    private final NewBedwars plugin;

    public NewBedwarsExpansion(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "newbedwars";
    }

    @Override
    public @NotNull String getAuthor() {
        return "l299l";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        // --- per-arena lookups: arena_<name>_<field> ---
        if (params.startsWith("arena_")) {
            String rest = params.substring("arena_".length());
            int lastUnderscore = rest.lastIndexOf('_');
            if (lastUnderscore > 0) {
                String arenaName = rest.substring(0, lastUnderscore);
                String field = rest.substring(lastUnderscore + 1);
                IArena arena = Arena.arenaByName.get(arenaName);
                if (arena == null) return "";
                switch (field) {
                    case "status": return arena.status().name().toLowerCase();
                    case "players": return String.valueOf(arena.getPlayers().size());
                    case "max_players": return String.valueOf(arena.getTeams().size() * arena.getMaxInTeam());
                    case "phase": return arena.getCurrentGamePhase();
                    default: return null;
                }
            }
            return null;
        }

        if (player == null) return "";

        // --- per-player lookups ---
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
            case "kills": {
                if (arena == null) return "0";
                GamePlayer gp = arena.getPlayer(player.getUniqueId());
                return gp != null ? String.valueOf(gp.getKills()) : "0";
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
            default:
                return null;
        }
    }
}
