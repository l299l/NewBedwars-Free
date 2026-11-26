package com.l299l.newbedwars.scoreboard;

import com.l299l.newbedwars.text.ColorCodes;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SidebarLines {
    private static final String TEAM_PREFIX = "nbw_l";

    private final String objectiveId;
    private final Scoreboard scoreboard;
    private final List<String> entries = new ArrayList<>();
    private final List<Team> teams = new ArrayList<>();
    private final List<String> texts = new ArrayList<>();

    public SidebarLines(String objectiveId, Scoreboard scoreboard) {
        this.objectiveId = objectiveId;
        this.scoreboard = scoreboard;
    }

    public static boolean usesTeam(String line, boolean legacyLimit) {
        return ColorCodes.hasHex(line) || (legacyLimit && line.length() > ScoreboardManager.LEGACY_ENTRY_LIMIT);
    }

    public static String teamEntry(int row) {
        return "§" + Integer.toHexString(row) + "§r";
    }

    public static boolean isLegacyLimited(List<String> lines) {
        for (String line : lines) {
            if (line.length() > ScoreboardManager.LEGACY_ENTRY_LIMIT) {
                return !VersionCompat.isAtLeast(1, 20, 3);
            }
        }
        return false;
    }

    public static List<String> prepare(List<String> lines) {
        List<String> fitted = ScoreboardManager.fitLines(lines);
        boolean legacy = isLegacyLimited(fitted);
        List<String> plain = new ArrayList<>();
        for (String line : fitted) {
            if (!usesTeam(line, legacy)) plain.add(line);
        }
        List<String> unique = ScoreboardManager.makeUnique(plain);
        List<String> out = new ArrayList<>(fitted.size());
        int p = 0;
        for (String line : fitted) {
            out.add(usesTeam(line, legacy) ? line : unique.get(p++));
        }
        return out;
    }

    public void apply(List<String> lines) {
        boolean legacy = isLegacyLimited(lines);
        int size = lines.size();
        String[] desired = new String[size];
        boolean[] team = new boolean[size];
        Set<String> keep = new HashSet<>();
        for (int i = 0; i < size; i++) {
            team[i] = usesTeam(lines.get(i), legacy);
            desired[i] = team[i] ? teamEntry(i) : lines.get(i);
            keep.add(desired[i]);
        }
        for (int i = 0; i < entries.size(); i++) {
            String old = entries.get(i);
            if (old != null && !keep.contains(old)) scoreboard.resetScores(old);
            Team oldTeam = teams.get(i);
            if (oldTeam != null && !(i < size && team[i])) {
                unregister(oldTeam);
                teams.set(i, null);
                texts.set(i, null);
            }
        }
        while (entries.size() < size) {
            entries.add(null);
            teams.add(null);
            texts.add(null);
        }
        Objective objective = scoreboard.getObjective(objectiveId);
        for (int i = 0; i < size; i++) {
            if (team[i]) applyTeam(i, lines.get(i), desired[i]);
            entries.set(i, desired[i]);
            if (objective != null) objective.getScore(desired[i]).setScore(size - i);
        }
        while (entries.size() > size) {
            int last = entries.size() - 1;
            entries.remove(last);
            teams.remove(last);
            texts.remove(last);
        }
    }

    private void applyTeam(int row, String line, String entry) {
        Team team = teams.get(row);
        if (team == null) {
            String name = TEAM_PREFIX + row;
            team = scoreboard.getTeam(name);
            if (team == null) team = scoreboard.registerNewTeam(name);
            teams.set(row, team);
            texts.set(row, null);
            team.addEntry(entry);
        }
        if (!line.equals(texts.get(row))) {
            LinePacker.Packed packed = LinePacker.pack(line);
            team.setPrefix(packed.prefix());
            team.setSuffix(packed.suffix());
            texts.set(row, line);
        }
    }

    public void clear() {
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            if (team != null) unregister(team);
        }
        entries.clear();
        teams.clear();
        texts.clear();
    }

    private static void unregister(Team team) {
        try {
            team.unregister();
        } catch (IllegalStateException ignored) {
        }
    }
}
