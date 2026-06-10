package com.l299l.newbedwars.scoreboard;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.bossbar.TextEffect;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class ScoreboardManager {
    private final HashMap<String, ScoreboardSave> scoreboards;
    private final FileConfiguration scoreboardConf;

    public ScoreboardManager(FileConfiguration scoreboardConf) {
        scoreboards = new HashMap<String, ScoreboardSave>();
        this.scoreboardConf = scoreboardConf;
    }

    public void loadScoreboards() {
        NewBedwars.plugin.getLogger().info("Loading scoreboards...");
        for (String key : Objects.requireNonNull(scoreboardConf.getConfigurationSection("Scoreboards")).getKeys(false)) {
            boolean enabled = scoreboardConf.getBoolean("Scoreboards." + key + ".Enabled");
            String title = scoreboardConf.getString("Scoreboards." + key + ".Title");
            String textEffect = scoreboardConf.getString("Scoreboards." + key + ".TitleEffect");
            ScoreboardSave scoreboardSave = new ScoreboardSave(enabled, title, TextEffect.valueOf(textEffect), scoreboardConf.getStringList("Scoreboards." + key + ".Lines"));
            scoreboards.put(key, scoreboardSave);
        }
        NewBedwars.plugin.getLogger().info("Loaded " + scoreboards.size() + " scoreboards!");
    }

    public NScoreboard createPlayerScoreboard(String name, IArena arena, Player player) {
        if (!scoreboards.containsKey(name)) {
            return null;
        }
        ScoreboardSave scoreboardSave = scoreboards.get(name);
        if (!scoreboardSave.enabled()) {
            return null;
        }
        String id = name + "-" + arena.getArenaName() + "-" + player.getName();
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewObjective(id, name);
        scoreboard.getObjective(id).setDisplayName(ChatColor.translateAlternateColorCodes('&', scoreboardSave.title()));
        scoreboard.getObjective(id).setDisplaySlot(DisplaySlot.SIDEBAR);
        TextEffect textEffect = scoreboardSave.textEffect();

        List<String> old = updateScoreboard(id, name, scoreboard, arena, player, null);

        player.setScoreboard(scoreboard);
        return new NScoreboard(id, name, scoreboard, this, arena, player, old, textEffect, scoreboardSave.title());
    }

    public List<String> updateScoreboard(String id, String name, Scoreboard scoreboard, IArena arena, Player player, List<String> old) {
        ScoreboardSave scoreboardSave = scoreboards.get(name);
        if (!scoreboardSave.enabled()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        int sep = 1;
        for (String line : scoreboardSave.lines()) {
            if (line.contains("/teams/")) {
                if (arena.getTeams() != null) {
                    for (String team : arena.getTeams().keySet()) {
                        Team t = arena.getTeam(team);
                        String yourTeam = "";
                        if (arena.getTeam(player) != null) {
                            yourTeam = arena.getTeam(player).equals(t) ? ChatColor.GRAY + "YOU" : "";
                        }
                        lines.add(setInfo(line.replace("/teams/", getTeamID(t))
                                        .replace("/bed/",  getTeamBedInfo(t))
                                        .replace("/yourteam/", yourTeam),
                                arena, player));
                    }
                }
            }else if (line.isEmpty()) {
                lines.add(" ".repeat(Math.max(1, sep)));
                sep++;
            }else {
                lines.add(setInfo(line, arena, player));
            }
        }
        if (old != null) {
            for (String line : old) {
                scoreboard.resetScores(line);
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            scoreboard.getObjective(id).getScore(lines.get(i)).setScore(lines.size() - i);
        }
        return lines;
    }

    private String setInfo(String line, IArena arena, Player player) {
        line = line.replace("/datetime/", TimeUtils.getActualDateTime());
        line = line.replace("/date/", TimeUtils.getActualDate());
        line = line.replace("/servername/", ChatColor.translateAlternateColorCodes('&', Properties.ServerName));
        if (arena != null) {
            line = line.replace("/map/", arena.getArenaName());
            line = line.replace("/players/", arena.getPlayers() == null ? "0" : String.valueOf(arena.getPlayers().size()));
            line = line.replace("/maxplayers/", arena.getTeams() == null ? "0" : String.valueOf(arena.getMaxInTeam() * arena.getTeams().size()));
            line = line.replace("/minplayers/", String.valueOf(arena.getMinPlayers()));
            line = line.replace("/status/", getInteractiveStatus(arena.status(), arena));
            line = line.replace("/gameTime/", TimeUtils.formatTime(arena.getGameTime()));
            line = line.replace("/phase/", arena.getNextGamePhase());
            line = line.replace("/phaseTime/", TimeUtils.formatTime(arena.getPhaseTime()));
        }
        if (player != null) {
            line = line.replace("/player/", player.getName());
            if (arena != null) {
                GamePlayer gp = arena.getPlayer(player.getUniqueId());
                if (gp != null) {
                    line = line.replace("/player.stats.kills/", String.valueOf(gp.getKills()));
                    line = line.replace("/player.stats.final_kills/", String.valueOf(gp.getFinalKills()));
                    line = line.replace("/player.stats.beds_broken/", String.valueOf(gp.getBedsBroken()));
                }
            }
            if (line.contains("/player.stats.")) {
                line = line.replaceAll("/player\\.stats\\.\\w+/", "0");
            }
        }
        return ChatColor.translateAlternateColorCodes('&', line);
    }

    private String getInteractiveStatus(GameStatus status, IArena arena) {
        return switch (status) {
            case waiting -> "Waiting...";
            case starting -> "Starting in " + TimeUtils.formatTime(arena.getPhaseTime());
            case playing -> "Playing";
            case ending -> "Viewing the battlefield";
            case restarting -> "Restarting";
        };
    }

    private String getTeamID(Team team) {
        return team.getColor().toString() + team.getName().substring(0, 1).toUpperCase() + " " + ChatColor.WHITE + team.getName();
    }

    private String getTeamBedInfo(Team team) {
        if (team == null) {
            return ChatColor.RED + "✗";
        }
        if (team.isBedDestroyed()) {
            if (team.getPlayers().isEmpty()) {
                return ChatColor.RED + "✗";
            }else {
                return ChatColor.YELLOW + String.valueOf(team.getPlayers().size());
            }
        }else {
            return ChatColor.GREEN + "✔";
        }
    }
}
