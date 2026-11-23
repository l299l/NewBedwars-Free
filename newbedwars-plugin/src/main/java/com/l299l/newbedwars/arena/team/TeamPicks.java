package com.l299l.newbedwars.arena.team;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamPicks {
    public enum Result { OK, FULL, UNBALANCED, UNKNOWN_TEAM }

    private final Map<UUID, String> picks = new LinkedHashMap<>();

    public String get(UUID player) {
        return picks.get(player);
    }

    public void set(UUID player, String team) {
        picks.remove(player);
        picks.put(player, team);
    }

    public boolean clear(UUID player) {
        return picks.remove(player) != null;
    }

    public void clearAll() {
        picks.clear();
    }

    public boolean isEmpty() {
        return picks.isEmpty();
    }

    public Map<UUID, String> all() {
        return Collections.unmodifiableMap(picks);
    }

    public static Result canPick(String team, Collection<String> teamNames, int groupSize,
                                 Map<String, Integer> pickedCounts, int maxInTeam, boolean balanced) {
        if (team == null || !teamNames.contains(team)) return Result.UNKNOWN_TEAM;
        int count = pickedCounts.getOrDefault(team, 0);
        if (count + groupSize > maxInTeam) return Result.FULL;
        if (balanced) {
            int min = Integer.MAX_VALUE;
            for (String name : teamNames) min = Math.min(min, pickedCounts.getOrDefault(name, 0));
            if (count > min) return Result.UNBALANCED;
        }
        return Result.OK;
    }
}
