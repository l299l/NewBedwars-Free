package com.l299l.newbedwars.tablist;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.placeholder.AddonPlaceholders;
import com.l299l.newbedwars.placeholder.PapiText;
import com.l299l.newbedwars.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

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

    public static String render(String template, Player viewer, IArena arena, Player target) {
        if (template == null) return "";
        String text = template;
        text = text.replace("{server}", Properties.ServerName == null ? "" : Properties.ServerName);
        if (text.contains("{online}")) text = text.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = text.replace("{player}", target == null ? "" : target.getName());
        Team team = arena != null && target != null ? arena.getTeam(target) : null;
        String teamName = team == null ? "" : team.getDisplayName();
        text = text.replace("{teamColor}", team == null ? "" : team.getColor().toString());
        text = text.replace("{teamName}", teamName);
        text = text.replace("{teamShort}", teamName.isEmpty() ? "" : teamName.substring(0, 1).toUpperCase());
        if (arena != null) {
            text = text.replace("{arena}", arena.getArenaName());
            text = text.replace("{players}", String.valueOf(arena.getActivePlayerCount()));
            text = text.replace("{maxPlayers}", String.valueOf(arena.getMaxPlayers()));
            text = text.replace("{minPlayers}", String.valueOf(arena.getMinPlayers()));
            text = text.replace("{phase}", String.valueOf(arena.getNextGamePhase()));
            text = text.replace("{phaseTime}", TimeUtils.formatTime(arena.getPhaseTime()));
            text = text.replace("{gameTime}", TimeUtils.formatTime(arena.getGameTime()));
            GamePlayer gp = target == null ? null : arena.getPlayer(target.getUniqueId());
            text = text.replace("{kills}", String.valueOf(gp == null ? 0 : gp.getKills()));
            text = text.replace("{finalKills}", String.valueOf(gp == null ? 0 : gp.getFinalKills()));
            text = text.replace("{bedsBroken}", String.valueOf(gp == null ? 0 : gp.getBedsBroken()));
        }
        text = AddonPlaceholders.apply(text, '{', '}', target == null ? viewer : target, arena);
        text = ChatColor.translateAlternateColorCodes('&', text);
        return PapiText.apply(viewer, text);
    }
}
