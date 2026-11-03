package com.l299l.newbedwars.arena.team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class TeamAssigner {
    private TeamAssigner() {
    }

    public static <T> List<List<T>> assign(List<? extends List<T>> groups, int teamCount, int maxInTeam,
                                           boolean random, Random rnd) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            result.add(new ArrayList<>());
        }
        if (teamCount == 0) return result;

        List<List<T>> ordered = new ArrayList<>(groups);
        ordered.sort(Comparator.comparingInt((List<T> g) -> g.size()).reversed());
        if (random) {

            int from = 0;
            while (from < ordered.size()) {
                int to = from;
                while (to < ordered.size() && ordered.get(to).size() == ordered.get(from).size()) to++;
                java.util.Collections.shuffle(ordered.subList(from, to), rnd);
                from = to;
            }
        }

        for (List<T> group : ordered) {
            int target = leastFilled(result, maxInTeam - group.size(), random, rnd);
            if (target >= 0) {
                result.get(target).addAll(group);
                continue;
            }
            for (T member : group) {
                int idx = leastFilled(result, maxInTeam - 1, random, rnd);
                if (idx < 0) break;
                result.get(idx).add(member);
            }
        }
        return result;
    }

    private static <T> int leastFilled(List<List<T>> teams, int maxSize, boolean random, Random rnd) {
        int best = Integer.MAX_VALUE;
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            int size = teams.get(i).size();
            if (size > maxSize) continue;
            if (size < best) {
                best = size;
                candidates.clear();
            }
            if (size == best) candidates.add(i);
        }
        if (candidates.isEmpty()) return -1;
        return random ? candidates.get(rnd.nextInt(candidates.size())) : candidates.get(0);
    }
}
