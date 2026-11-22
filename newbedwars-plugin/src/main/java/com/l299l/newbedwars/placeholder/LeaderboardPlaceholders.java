package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

public final class LeaderboardPlaceholders {
    public static final int MAX_POSITION = 10;

    record Entry(String name, int value) {
    }

    private record Board(BiFunction<PlayerManager, Integer, List<Map.Entry<String, PlayerStats>>> top,
                         ToIntFunction<PlayerStats> value) {
    }

    private static final Map<String, Board> BOARDS = new LinkedHashMap<>();

    static {
        BOARDS.put("wins", new Board(PlayerManager::getTopByWins, PlayerStats::wins));
        BOARDS.put("kills", new Board(PlayerManager::getTopByKills, PlayerStats::kills));
        BOARDS.put("final_kills", new Board(PlayerManager::getTopByFinalKills, PlayerStats::finalKills));
        BOARDS.put("beds", new Board(PlayerManager::getTopByBeds, PlayerStats::bedsBroken));
    }

    private static volatile Map<String, List<Entry>> snapshot = Map.of();
    private static BukkitTask task;

    private LeaderboardPlaceholders() {
    }

    public static List<String> stats() {
        return List.copyOf(BOARDS.keySet());
    }

    public static synchronized void start(Plugin plugin, PlayerManager players, int refreshSeconds) {
        stop();
        refresh(players);
        long period = 20L * Math.max(5, refreshSeconds);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> refresh(players), 40L, period);
    }

    public static synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    static void refresh(PlayerManager players) {
        Map<String, List<Entry>> next = new LinkedHashMap<>();
        for (Map.Entry<String, Board> b : BOARDS.entrySet()) {
            List<Entry> entries = new ArrayList<>();
            List<Map.Entry<String, PlayerStats>> top = b.getValue().top().apply(players, MAX_POSITION);
            if (top != null) {
                for (Map.Entry<String, PlayerStats> e : top) {
                    if (e == null || e.getValue() == null) continue;
                    entries.add(new Entry(e.getKey(), b.getValue().value().applyAsInt(e.getValue())));
                }
            }
            next.put(b.getKey(), List.copyOf(entries));
        }
        snapshot = Map.copyOf(next);
    }

    static void clear() {
        snapshot = Map.of();
    }

    public static String resolve(String params, String emptyName) {
        int last = params.lastIndexOf('_');
        if (last <= 0) return null;
        String field = params.substring(last + 1);
        if (!field.equals("name") && !field.equals("value")) return null;
        String rest = params.substring(0, last);
        int split = rest.lastIndexOf('_');
        if (split <= 0) return null;
        String stat = rest.substring(0, split);
        if (!BOARDS.containsKey(stat)) return null;
        int position;
        try {
            position = Integer.parseInt(rest.substring(split + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (position < 1 || position > MAX_POSITION) return null;
        List<Entry> board = snapshot.getOrDefault(stat, List.of());
        if (position > board.size()) return field.equals("name") ? emptyName : "0";
        Entry entry = board.get(position - 1);
        return field.equals("name") ? entry.name() : String.valueOf(entry.value());
    }
}
