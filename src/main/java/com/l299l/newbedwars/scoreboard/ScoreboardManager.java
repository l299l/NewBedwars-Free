package com.l299l.newbedwars.scoreboard;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.text.TextEffect;
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
    /** Every player has their own {@link Scoreboard}, so one short name is enough (1.17 limits objective names to 16 characters). */
    private static final String OBJECTIVE_ID = "nbw";
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
            ScoreboardSave scoreboardSave = new ScoreboardSave(enabled, title, parseEffect(textEffect), scoreboardConf.getStringList("Scoreboards." + key + ".Lines"));
            scoreboards.put(key, scoreboardSave);
        }
        NewBedwars.plugin.getLogger().info("Loaded " + scoreboards.size() + " scoreboards!");
    }

    private static TextEffect parseEffect(String name) {
        if (name == null) return TextEffect.NONE;
        try {
            return TextEffect.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TextEffect.NONE;
        }
    }

    public NScoreboard createPlayerScoreboard(String name, IArena arena, Player player) {
        if (!scoreboards.containsKey(name)) {
            return null;
        }
        ScoreboardSave scoreboardSave = scoreboards.get(name);
        if (!scoreboardSave.enabled()) {
            return null;
        }
        String id = OBJECTIVE_ID;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewObjective(id, "dummy");
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
            line = line.replace("/players/", String.valueOf(arena.getActivePlayerCount()));
            line = line.replace("/maxplayers/", String.valueOf(arena.getMaxPlayers()));
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
        String name = team.getName();
        String initial = name.isEmpty() ? "" : name.substring(0, 1).toUpperCase();
        return team.getColor().toString() + initial + " " + ChatColor.WHITE + name;
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
