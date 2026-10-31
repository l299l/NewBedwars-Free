package com.l299l.newbedwars.arena.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The map vote of one waiting lobby: one vote per player (changing replaces it), keyed by arena name
 * (case-insensitive). The winner has the most votes; a tie, or no votes at all, is decided randomly.
 */
public final class MapVote {
    private final Map<UUID, String> votes = new HashMap<>();
    private boolean closed;

    /** Records or changes {@code voter}'s vote. @return false when voting is closed */
    public boolean vote(UUID voter, String arena) {
        if (closed) return false;
        votes.put(voter, key(arena));
        return true;
    }

    public void removeVoter(UUID voter) {
        votes.remove(voter);
    }

    /** Drops every vote for {@code arena} (it became unavailable). */
    public void removeOption(String arena) {
        String k = key(arena);
        votes.values().removeIf(k::equals);
    }

    /** The arena {@code voter} voted for (lower case), or null. */
    public String voteOf(UUID voter) {
        return votes.get(voter);
    }

    public int votes(String arena) {
        String k = key(arena);
        int n = 0;
        for (String v : votes.values()) {
            if (v.equals(k)) n++;
        }
        return n;
    }

    /** Vote count per option, in option order; options without votes have 0, votes for other arenas are ignored. */
    public Map<String, Integer> counts(Collection<String> options) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String option : options) counts.put(option, votes(option));
        return counts;
    }

    /**
     * The option with the most votes; ties and "no votes" pick randomly among the tied options.
     * @return null when there are no options
     */
    public String winner(List<String> options, Random random) {
        if (options.isEmpty()) return null;
        int best = -1;
        List<String> tied = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts(options).entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                tied.clear();
                tied.add(e.getKey());
            } else if (e.getValue() == best) {
                tied.add(e.getKey());
            }
        }
        return tied.get(random.nextInt(tied.size()));
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        closed = true;
    }

    private static String key(String arena) {
        return arena.toLowerCase(Locale.ROOT);
    }
}
