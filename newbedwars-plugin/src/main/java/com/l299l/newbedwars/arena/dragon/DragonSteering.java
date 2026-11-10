package com.l299l.newbedwars.arena.dragon;

import org.bukkit.util.Vector;

public final class DragonSteering {
    private static final double EPSILON = 1.0E-6;

    private DragonSteering() {}

    public static Vector turn(Vector current, Vector desired, double maxRadians) {
        Vector from = normalizedOr(current, new Vector(1, 0, 0));
        Vector to = normalizedOr(desired, from);
        double dot = Math.max(-1, Math.min(1, from.dot(to)));
        double angle = Math.acos(dot);
        if (angle <= maxRadians) return to;
        Vector axis = from.getCrossProduct(to);
        if (axis.lengthSquared() < EPSILON) {
            axis = Math.abs(from.getY()) > 0.9 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        }
        return from.rotateAroundAxis(axis.normalize(), maxRadians).normalize();
    }

    public static Vector heading(Vector position, Vector goal, Vector direction, double maxTurnDegrees) {
        Vector toGoal = goal.clone().subtract(position);
        if (toGoal.lengthSquared() < EPSILON) return normalizedOr(direction, new Vector(1, 0, 0));
        return turn(direction, toGoal, Math.toRadians(maxTurnDegrees));
    }

    public static float yaw(Vector direction) {
        double yaw = Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ())) + 180;
        return (float) wrap(yaw);
    }

    public static Vector lead(Vector from, Vector target, Vector targetVelocity, double projectileSpeed) {
        double ticks = target.distance(from) / Math.max(0.1, projectileSpeed);
        return target.clone().add(targetVelocity.clone().multiply(ticks));
    }

    static double wrap(double degrees) {
        double d = degrees % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    private static Vector normalizedOr(Vector v, Vector fallback) {
        if (v == null || v.lengthSquared() < EPSILON) return fallback.clone().normalize();
        return v.clone().normalize();
    }
}
