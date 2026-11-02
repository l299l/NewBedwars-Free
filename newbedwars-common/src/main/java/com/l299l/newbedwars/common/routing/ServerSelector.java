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

/**
 * Picks where a join request goes. Arenas of servers with a fresh state that fit the request come first,
 * the fullest first (same rule as a local {@code /bw join}), ties by arena name. Game servers without a
 * fresh state come last as "blind" candidates: the player is sent with the request itself and the game
 * server decides (and rejects when nothing fits).
 */
public final class ServerSelector {
    /** Longest group/arena target a request may carry. */
    public static final int MAX_TARGET_LENGTH = 64;

    private ServerSelector() {}

    /** A game server known to the proxy. {@code state} may be null. */
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

    /** One place to try: a server and the arena there, or a null arena for a blind try. */
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

    /**
     * All places to try, best first.
     *
     * @param needed  players joining together (party size, at least 1)
     * @param exclude server ids that already rejected this request
     */
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

    /** Where {@code player} can rejoin: the arena holding their (unexpired) rejoin slot, or null. */
    public static Choice findRejoin(Collection<Candidate> servers, java.util.UUID player, long now) {
        for (Candidate c : servers) {
            if (c == null || c.state == null || c.state.arenas == null) continue;
            for (ArenaInfo a : c.state.arenas) {
                if (a != null && a.name != null && a.rejoinSlot(player, now) != null) return new Choice(c.serverId, a.name);
            }
        }
        return null;
    }

    /** Where the running game {@code arena} can be watched: a playing or ending arena with that name, or null. */
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

    /** Whether {@code a} is what the request aims at (ignores whether it has room). */
    public static boolean matchesTarget(ArenaInfo a, TargetKind kind, String target) {
        if (kind == TargetKind.REJOIN || kind == TargetKind.SPECTATE) return false; // routed by slot / running game
        if (kind == null || kind == TargetKind.ANY || target == null || target.isEmpty()) return true;
        if (kind == TargetKind.ARENA) return a.name.equalsIgnoreCase(target);
        // GROUP: the arena group first, else a name pattern ("solo*") or a team size ("4x1").
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
                // not a size, fall through to the pattern
            }
        }
        return glob(target, a.name);
    }

    /** Same glob rule as local join NPCs: '*' = any characters, everything else literal, case-insensitive. */
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
