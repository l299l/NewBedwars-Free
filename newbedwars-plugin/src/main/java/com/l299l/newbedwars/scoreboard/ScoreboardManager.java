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

    public List<String> getIds() {
        List<String> ids = new ArrayList<>(scoreboards.keySet());
        Collections.sort(ids);
        return ids;
    }

    public String find(String id) {
        if (id == null) return null;
        if (scoreboards.containsKey(id)) return id;
        for (String key : scoreboards.keySet()) {
            if (key.equalsIgnoreCase(id.trim())) return key;
        }
        return null;
    }

    public boolean isEnabled(String id) {
        ScoreboardSave save = id == null ? null : scoreboards.get(id);
        return save != null && save.enabled();
    }

    public String effectiveTemplate(String base, IArena arena, Player player) {
        if (ScoreboardOverrides.isHidden(player)) return null;
        String forced = ScoreboardOverrides.get(player, arena == null ? null : arena.getArenaName());
        return forced == null ? base : forced;
    }

    public NScoreboard createPlayerScoreboard(String name, IArena arena, Player player) {
        ScoreboardSave scoreboardSave = scoreboards.get(name);
        if (scoreboardSave == null || !scoreboardSave.enabled()) {
            return null;
        }
        String id = OBJECTIVE_ID;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewObjective(id, "dummy");
        scoreboard.getObjective(id).setDisplayName(ChatColor.translateAlternateColorCodes('&', scoreboardSave.title()));
        scoreboard.getObjective(id).setDisplaySlot(DisplaySlot.SIDEBAR);
        NScoreboard board = new NScoreboard(id, name, scoreboard, this, arena, player, scoreboardSave.textEffect(), scoreboardSave.title());
        board.update();
        player.setScoreboard(scoreboard);
        return board;
    }

    public String titleOf(String name) {
        ScoreboardSave save = scoreboards.get(name);
        return save == null ? "" : ChatColor.translateAlternateColorCodes('&', save.title());
    }

    public List<String> render(String name, IArena arena, Player player) {
        ScoreboardSave scoreboardSave = scoreboards.get(name);
        if (scoreboardSave == null || !scoreboardSave.enabled()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        int sep = 1;
        for (String line : scoreboardSave.lines()) {
            if (line.contains("/teams/") || line.contains("{teams}")) {
                if (arena.getTeams() != null) {
                    for (String team : arena.getTeams().keySet()) {
                        Team t = arena.getTeam(team);
                        String yourTeam = "";
                        if (arena.getTeam(player) != null) {
                            yourTeam = arena.getTeam(player).equals(t) ? ChatColor.GRAY + text(player, "ScoreboardYou") : "";
                        }
                        lines.add(setInfo(com.l299l.newbedwars.text.Tokens.apply(line, java.util.Map.of(
                                        "teams", getTeamID(t),
                                        "bed", getTeamBedInfo(t),
                                        "yourteam", yourTeam)),
                                arena, player));
                    }
                }
            } else if ((line.contains("/server/") || line.contains("{server}")) && com.l299l.newbedwars.network.NetworkManager.get() == null) {

                continue;
            }else if (line.isEmpty()) {
                lines.add(" ".repeat(Math.max(1, sep)));
                sep++;
            }else {
                lines.add(setInfo(line, arena, player));
            }
        }
        return fitLines(lines);
    }

    public static final int MAX_LINES = 15;
    public static final int LEGACY_ENTRY_LIMIT = 40;

    public static List<String> fitLines(List<String> lines) {
        List<String> out = new ArrayList<>(lines);
        for (int i = out.size() - 1; i >= 0 && out.size() > MAX_LINES; i--) {
            if (ChatColor.stripColor(out.get(i)).isBlank()) out.remove(i);
        }
        while (out.size() > MAX_LINES) out.remove(out.size() - 1);
        return out;
    }

    public static String truncateEntry(String entry, int max) {
        if (max <= 0) return "";
        if (entry.length() <= max) return entry;
        String cut = entry.substring(0, max);
        if (cut.endsWith("§")) cut = cut.substring(0, cut.length() - 1);
        return cut;
    }

    public static List<String> makeUnique(List<String> lines, int max) {
        List<String> out = new ArrayList<>(lines.size());
        java.util.Set<String> seen = new java.util.HashSet<>();
        String reset = ChatColor.RESET.toString();
        for (String line : lines) {
            String candidate = truncateEntry(line, max);
            int n = 0;
            while (!seen.add(candidate)) {
                n++;
                candidate = truncateEntry(line, max - 2 * n) + reset.repeat(n);
            }
            out.add(candidate);
        }
        return out;
    }

    public static List<String> prepare(List<String> lines) {
        List<String> fitted = fitLines(lines);
        boolean tooLong = false;
        for (String line : fitted) {
            if (line.length() > LEGACY_ENTRY_LIMIT) {
                tooLong = true;
                break;
            }
        }
        if (tooLong && !com.l299l.newbedwars.version.VersionCompat.isAtLeast(1, 20, 3)) {
            return makeUnique(fitted, LEGACY_ENTRY_LIMIT);
        }
        return makeUnique(fitted);
    }

    public void applyLines(String id, Scoreboard scoreboard, List<String> lines, List<String> old) {
        if (old != null) {
            java.util.Set<String> keep = new java.util.HashSet<>(lines);
            for (String line : old) {
                if (!keep.contains(line)) scoreboard.resetScores(line);
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            scoreboard.getObjective(id).getScore(lines.get(i)).setScore(lines.size() - i);
        }
    }

    public static List<String> makeUnique(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String line : lines) {
            String unique = line;
            while (!seen.add(unique)) unique += ChatColor.RESET;
            out.add(unique);
        }
        return out;
    }

    private String setInfo(String line, IArena arena, Player player) {
        java.util.Map<String, String> v = new java.util.HashMap<>();
        v.put("datetime", TimeUtils.getActualDateTime());
        v.put("date", TimeUtils.getActualDate());
        v.put("servername", ChatColor.translateAlternateColorCodes('&', Properties.ServerName));
        if (line.contains("/server/") || line.contains("{server}")) {
            com.l299l.newbedwars.network.NetworkManager network = com.l299l.newbedwars.network.NetworkManager.get();
            v.put("server", network == null ? "" : network.getServerId());
        }
        if (arena != null) {
            v.put("map", arena.getArenaName());
            v.put("players", String.valueOf(arena.getActivePlayerCount()));
            v.put("maxplayers", String.valueOf(arena.getMaxPlayers()));
            v.put("minplayers", String.valueOf(arena.getMinPlayers()));
            v.put("status", getInteractiveStatus(arena.status(), arena, player));
            v.put("gameTime", TimeUtils.formatTime(arena.getGameTime()));
            v.put("phase", arena.getNextGamePhase());
            v.put("phaseTime", TimeUtils.formatTime(arena.getPhaseTime()));
        }
        if (player != null) {
            v.put("player", player.getName());
            if (arena != null) {
                GamePlayer gp = arena.getPlayer(player.getUniqueId());
                if (gp != null) {
                    v.put("player.stats.kills", String.valueOf(gp.getKills()));
                    v.put("player.stats.final_kills", String.valueOf(gp.getFinalKills()));
                    v.put("player.stats.beds_broken", String.valueOf(gp.getBedsBroken()));
                }
            }
        }
        line = com.l299l.newbedwars.text.Tokens.apply(line, v);
        if (player != null && (line.contains("/player.stats.") || line.contains("{player.stats."))) {
            line = line.replaceAll("/player\\.stats\\.\\w+/|\\{player\\.stats\\.\\w+}", "0");
        }
        line = com.l299l.newbedwars.placeholder.AddonPlaceholders.apply(line, '/', '/', player, arena);
        return com.l299l.newbedwars.placeholder.PapiText.apply(player, ChatColor.translateAlternateColorCodes('&', line));
    }

    private String getInteractiveStatus(GameStatus status, IArena arena, Player player) {
        return switch (status) {
            case waiting -> text(player, "ScoreboardStatusWaiting");
            case starting -> text(player, "ScoreboardStatusStarting").replace("/time/", TimeUtils.formatTime(arena.getPhaseTime()));
            case playing -> text(player, "ScoreboardStatusPlaying");
            case ending -> text(player, "ScoreboardStatusEnding");
            case restarting -> text(player, "ScoreboardStatusRestarting");
        };
    }

    private static String text(Player player, String key) {
        return NewBedwars.plugin.getMessages().getMsg(player, key);
    }

    private String getTeamID(Team team) {
        String name = team.getDisplayName();
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
