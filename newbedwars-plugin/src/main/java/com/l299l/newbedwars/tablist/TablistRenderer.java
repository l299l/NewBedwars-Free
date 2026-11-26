package com.l299l.newbedwars.tablist;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.arena.team.TeamSelectorManager;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.placeholder.AddonPlaceholders;
import com.l299l.newbedwars.placeholder.PapiText;
import com.l299l.newbedwars.text.Tokens;
import com.l299l.newbedwars.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TablistRenderer {
    private TablistRenderer() {
    }

    public static String join(List<String> lines, Player viewer, IArena arena, Player target) {
        if (lines == null || lines.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) out.append('\n');
            out.append(render(lines.get(i), viewer, arena, target));
        }
        return out.toString();
    }

    private static Team pickedTeam(IArena arena, Player target) {
        TeamSelectorManager selector = TeamSelectorManager.get();
        if (selector == null || !selector.enabled(arena)) return null;
        GameStatus status = arena.status();
        if (status != GameStatus.waiting && status != GameStatus.starting) return null;
        String picked = selector.pickedTeam(target, arena);
        return picked == null || arena.getTeams() == null ? null : arena.getTeams().get(picked);
    }

    public static String render(String template, Player viewer, IArena arena, Player target) {
        if (template == null) return "";
        template = com.l299l.newbedwars.text.LangTokens.apply(template, viewer);
        Map<String, String> v = new HashMap<>();
        v.put("server", Properties.ServerName == null ? "" : Properties.ServerName);
        if (template.contains("online")) v.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        v.put("player", target == null ? "" : target.getName());
        Team team = arena != null && target != null ? arena.getTeam(target) : null;
        if (team == null && arena != null && target != null) team = pickedTeam(arena, target);
        String teamName = team == null ? "" : team.getDisplayName();
        v.put("teamColor", team == null ? "" : team.getColor().toString());
        v.put("teamName", teamName);
        v.put("teamShort", teamName.isEmpty() ? "" : teamName.substring(0, 1).toUpperCase());
        if (arena != null) {
            v.put("arena", arena.getArenaName());
            v.put("players", String.valueOf(arena.getActivePlayerCount()));
            v.put("maxPlayers", String.valueOf(arena.getMaxPlayers()));
            v.put("minPlayers", String.valueOf(arena.getMinPlayers()));
            v.put("phase", String.valueOf(arena.getNextGamePhase()));
            v.put("phaseTime", TimeUtils.formatTime(arena.getPhaseTime()));
            v.put("gameTime", TimeUtils.formatTime(arena.getGameTime()));
            GamePlayer gp = target == null ? null : arena.getPlayer(target.getUniqueId());
            v.put("kills", String.valueOf(gp == null ? 0 : gp.getKills()));
            v.put("finalKills", String.valueOf(gp == null ? 0 : gp.getFinalKills()));
            v.put("bedsBroken", String.valueOf(gp == null ? 0 : gp.getBedsBroken()));
        }
        String text = Tokens.apply(template, v);
        text = AddonPlaceholders.apply(text, '{', '}', target == null ? viewer : target, arena);
        text = ChatColor.translateAlternateColorCodes('&', text);
        return PapiText.apply(viewer, text);
    }
}
