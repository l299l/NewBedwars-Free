package com.l299l.newbedwars.arena.dragon;

import org.bukkit.util.Vector;

import java.util.List;

public final class DragonTargeting {
    public static final double NEAR_BED = 15;

    public enum Kind { PLAYER, BED, DRAGON }

    public record Target(Object id, Kind kind, Vector position, Vector velocity) {
        public Target(Object id, Kind kind, Vector position) {
            this(id, kind, position, new Vector());
        }
    }

    public record Candidates(List<Target> players, List<Target> beds, List<Target> dragons, List<Vector> aliveBeds) {
        public static final Candidates NONE = new Candidates(List.of(), List.of(), List.of(), List.of());
    }

    private DragonTargeting() {}

    public static Target choose(Target current, Vector from, Candidates c, double range, boolean reevaluate) {
        if (current != null && !reevaluate) {
            Target refreshed = find(current, c);
            if (refreshed != null && within(from, refreshed, range)) return refreshed;
        }
        return pick(from, c, range);
    }

    public static Target pick(Vector from, Candidates c, double range) {
        Target best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Target p : c.players()) {
            if (!nearAnyBed(p.position(), c.aliveBeds())) continue;
            double d = p.position().distanceSquared(from);
            if (d < bestDistance && within(from, p, range)) {
                best = p;
                bestDistance = d;
            }
        }
        if (best != null) return best;
        for (List<Target> group : List.of(c.players(), c.beds(), c.dragons())) {
            Target nearest = nearest(from, group, range);
            if (nearest != null) return nearest;
        }
        return null;
    }

    private static Target find(Target current, Candidates c) {
        List<Target> group = switch (current.kind()) {
            case PLAYER -> c.players();
            case BED -> c.beds();
            case DRAGON -> c.dragons();
        };
        for (Target t : group) {
            if (t.id().equals(current.id())) return t;
        }
        return null;
    }

    private static Target nearest(Vector from, List<Target> group, double range) {
        Target best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Target t : group) {
            double d = t.position().distanceSquared(from);
            if (d < bestDistance && within(from, t, range)) {
                best = t;
                bestDistance = d;
            }
        }
        return best;
    }

    private static boolean nearAnyBed(Vector position, List<Vector> beds) {
        for (Vector bed : beds) {
            if (bed.distanceSquared(position) <= NEAR_BED * NEAR_BED) return true;
        }
        return false;
    }

    private static boolean within(Vector from, Target t, double range) {
        return t.position().distanceSquared(from) <= range * range;
    }
}
