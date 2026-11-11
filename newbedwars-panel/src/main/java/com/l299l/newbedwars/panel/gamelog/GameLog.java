package com.l299l.newbedwars.panel.gamelog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

public final class GameLog {
    public static final int MAX_ENTRIES = 2000;

    public enum Type {
        JOIN, REJOIN, LEAVE, CHAT, TEAM_CHAT, KILL, FINAL_KILL, DEATH, BED, BEDS_DESTROYED, PHASE, GENERATOR,
        UPGRADE, TEAM_OUT, START, END, ADMIN, COMMAND
    }

    public record Entry(long seq, long time, Type type, String player, String team, String teamColor,
                        String target, String targetColor, String detail) {}

    public record Page(long seq, boolean truncated, List<Entry> entries) {}

    private final LongSupplier clock;
    private final Map<String, Deque<Entry>> arenas = new HashMap<>();
    private final Map<String, Long> resetAt = new HashMap<>();
    private long seq;

    public GameLog(LongSupplier clock) {
        this.clock = clock;
    }

    private static String key(String arena) {
        return arena.toLowerCase(Locale.ROOT);
    }

    public synchronized Entry add(String arena, Type type, String player, String team, String teamColor,
                                  String target, String targetColor, String detail) {
        Entry entry = new Entry(++seq, clock.getAsLong(), type, player, team, teamColor, target, targetColor, detail);
        Deque<Entry> log = arenas.computeIfAbsent(key(arena), k -> new ArrayDeque<>());
        log.addLast(entry);
        while (log.size() > MAX_ENTRIES) log.removeFirst();
        return entry;
    }

    public synchronized void clear(String arena) {
        arenas.remove(key(arena));
        resetAt.remove(key(arena));
    }

    public synchronized void markReset(String arena) {
        resetAt.put(key(arena), seq);
    }

    public synchronized void newGame(String arena) {
        Long marker = resetAt.remove(key(arena));
        Deque<Entry> log = arenas.get(key(arena));
        if (marker == null || log == null) return;
        while (!log.isEmpty() && log.peekFirst().seq() <= marker) log.removeFirst();
    }

    public synchronized Page after(String arena, long after, int limit) {
        Deque<Entry> log = arenas.get(key(arena));
        if (log == null || log.isEmpty()) return new Page(seq, false, List.of());
        boolean truncated = after > 0 && log.peekFirst().seq() > after + 1;
        List<Entry> out = new ArrayList<>();
        for (Entry entry : log) {
            if (entry.seq() > after) out.add(entry);
        }
        if (out.size() > limit) {
            out = new ArrayList<>(out.subList(out.size() - limit, out.size()));
            truncated = true;
        }
        return new Page(seq, truncated, out);
    }
}
