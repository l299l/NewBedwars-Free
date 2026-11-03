package com.l299l.newbedwars.common.routing;

import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ServerSelector {
    public static final int MAX_TARGET_LENGTH = 64;

    private ServerSelector() {}

    public static final class Candidate {
        public final String serverId;
        public final ServerState state;
        public final boolean fresh;

        public Candidate(String serverId, ServerState state, boolean fresh) {
            this.serverId = serverId;
            this.state = state;
            this.fresh = fresh;
        }
    }

    public static final class Choice {
        public final String serverId;
        public final String arena;

        public Choice(String serverId, String arena) {
            this.serverId = serverId;
            this.arena = arena;
        }

        public boolean isBlind() {
            return arena == null;
        }

        @Override
        public String toString() {
            return serverId + (arena == null ? "" : "/" + arena);
        }
    }

    public static List<Choice> choose(Collection<Candidate> servers, TargetKind kind, String target,
                                      int needed, Set<String> exclude) {
        int n = Math.max(1, needed);
        List<Match> matches = new ArrayList<>();
        List<Choice> blind = new ArrayList<>();
        for (Candidate c : servers) {
            if (c == null || c.serverId == null || exclude.contains(c.serverId)) continue;
            if (!c.fresh || c.state == null) {
                blind.add(new Choice(c.serverId, null));
                continue;
            }
            if (c.state.restarting) continue;
            if (c.state.arenas == null) continue;
            for (ArenaInfo a : c.state.arenas) {
                if (a == null || a.name == null) continue;
                if (matchesTarget(a, kind, target) && a.fits(n)) matches.add(new Match(c.serverId, a));
            }
        }
        matches.sort(Comparator.<Match>comparingInt(m -> m.arena.players).reversed()
                .thenComparing(m -> m.arena.name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(m -> m.serverId));
        blind.sort(Comparator.comparing(ch -> ch.serverId));
        List<Choice> out = new ArrayList<>();
        for (Match m : matches) out.add(new Choice(m.serverId, m.arena.name));
        out.addAll(blind);
        return out;
    }

    public static Choice findRejoin(Collection<Candidate> servers, java.util.UUID player, long now) {
        for (Candidate c : servers) {
            if (c == null || c.state == null || c.state.arenas == null) continue;
            for (ArenaInfo a : c.state.arenas) {
                if (a != null && a.name != null && a.rejoinSlot(player, now) != null) return new Choice(c.serverId, a.name);
            }
        }
        return null;
    }

    public static Choice findRunning(Collection<Candidate> servers, String arena) {
        if (arena == null) return null;
        Choice best = null;
        for (Candidate c : servers) {
            if (c == null || c.state == null || c.state.arenas == null) continue;
            for (ArenaInfo a : c.state.arenas) {
                if (a == null || a.name == null || !a.name.equalsIgnoreCase(arena)) continue;
                if (a.state != ArenaInfo.State.PLAYING && a.state != ArenaInfo.State.ENDING) continue;
                if (best == null || c.fresh) best = new Choice(c.serverId, a.name);
            }
        }
        return best;
    }

    public static boolean matchesTarget(ArenaInfo a, TargetKind kind, String target) {
        if (kind == TargetKind.REJOIN || kind == TargetKind.SPECTATE) return false;
        if (kind == null || kind == TargetKind.ANY || target == null || target.isEmpty()) return true;
        if (kind == TargetKind.ARENA) return a.name.equalsIgnoreCase(target);

        if (a.group != null && a.group.equalsIgnoreCase(target)) return true;
        return matchesPatternOrSize(a, target);
    }

    private static boolean matchesPatternOrSize(ArenaInfo a, String target) {
        String t = target.toLowerCase();
        int x = t.indexOf('x');
        if (x > 0 && x < t.length() - 1) {
            try {
                int teams = Integer.parseInt(t.substring(0, x));
                int perTeam = Integer.parseInt(t.substring(x + 1));
                return a.teams == teams && a.maxInTeam == perTeam;
            } catch (NumberFormatException ignored) {

            }
        }
        return glob(target, a.name);
    }

    static boolean glob(String pattern, String text) {
        String p = pattern.toLowerCase(Locale.ROOT);
        String t = text.toLowerCase(Locale.ROOT);
        int pi = 0;
        int ti = 0;
        int star = -1;
        int mark = 0;
        while (ti < t.length()) {
            if (pi < p.length() && p.charAt(pi) == '*') {
                star = pi++;
                mark = ti;
            } else if (pi < p.length() && p.charAt(pi) == t.charAt(ti)) {
                pi++;
                ti++;
            } else if (star >= 0) {
                pi = star + 1;
                ti = ++mark;
            } else {
                return false;
            }
        }
        while (pi < p.length() && p.charAt(pi) == '*') pi++;
        return pi == p.length();
    }

    private static final class Match {
        final String serverId;
        final ArenaInfo arena;

        Match(String serverId, ArenaInfo arena) {
            this.serverId = serverId;
            this.arena = arena;
        }
    }
}
