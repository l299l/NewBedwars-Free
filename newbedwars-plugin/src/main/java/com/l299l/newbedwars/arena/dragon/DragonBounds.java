package com.l299l.newbedwars.arena.dragon;

import org.bukkit.util.Vector;

import java.util.Collection;

public final class DragonBounds {
    private static final double FLOOR_ABOVE_LOWEST = 3;
    private static final double CEILING_ABOVE_PATROL = 20;
    private static final double CEILING_BELOW_WORLD_MAX = 5;
    private static final double TARGET_VERTICAL_SLACK = 10;
    private static final double FALLBACK_RADIUS = 20;
    private static final double AUTO_RADIUS_SHARE = 0.6;

    private final double minX, maxX, minY, maxY, minZ, maxZ;
    private final Vector center;
    private final double patrolRadius;

    private DragonBounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ,
                         Vector center, double patrolRadius) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.center = center;
        this.patrolRadius = patrolRadius;
    }

    public static DragonBounds of(Collection<Vector> ground, Collection<Vector> area, double margin,
                                  double worldMinY, double worldMaxY, double patrolHeight, double patrolRadius) {
        if (ground == null || ground.isEmpty()) return null;
        double loX = Double.MAX_VALUE, hiX = -Double.MAX_VALUE;
        double loY = Double.MAX_VALUE, hiY = -Double.MAX_VALUE;
        double loZ = Double.MAX_VALUE, hiZ = -Double.MAX_VALUE;
        double sumX = 0, sumZ = 0;
        for (Vector p : ground) {
            loX = Math.min(loX, p.getX());
            hiX = Math.max(hiX, p.getX());
            loY = Math.min(loY, p.getY());
            hiY = Math.max(hiY, p.getY());
            loZ = Math.min(loZ, p.getZ());
            hiZ = Math.max(hiZ, p.getZ());
            sumX += p.getX();
            sumZ += p.getZ();
        }
        double radius = patrolRadius > 0 ? patrolRadius : Math.min(hiX - loX, hiZ - loZ) / 2 * AUTO_RADIUS_SHARE;
        if (radius < 1) radius = FALLBACK_RADIUS;
        if (area != null) {
            for (Vector p : area) {
                loX = Math.min(loX, p.getX());
                hiX = Math.max(hiX, p.getX());
                loZ = Math.min(loZ, p.getZ());
                hiZ = Math.max(hiZ, p.getZ());
            }
        }
        double ceiling = worldMaxY - CEILING_BELOW_WORLD_MAX;
        double floor = Math.max(worldMinY + 1, loY + FLOOR_ABOVE_LOWEST);
        double top = Math.min(ceiling, hiY + patrolHeight + CEILING_ABOVE_PATROL);
        if (top < floor) top = floor;
        double patrolY = Math.max(floor, Math.min(top, hiY + patrolHeight));
        Vector center = new Vector(sumX / ground.size(), patrolY, sumZ / ground.size());
        return new DragonBounds(loX - margin, hiX + margin, floor, top, loZ - margin, hiZ + margin, center, radius);
    }

    public Vector clamp(Vector v) {
        return new Vector(
                Math.max(minX, Math.min(maxX, v.getX())),
                Math.max(minY, Math.min(maxY, v.getY())),
                Math.max(minZ, Math.min(maxZ, v.getZ())));
    }

    public boolean contains(Vector v) {
        return v.getX() >= minX && v.getX() <= maxX
                && v.getY() >= minY && v.getY() <= maxY
                && v.getZ() >= minZ && v.getZ() <= maxZ;
    }

    public boolean holdsTarget(Vector v) {
        return v.getX() >= minX && v.getX() <= maxX
                && v.getZ() >= minZ && v.getZ() <= maxZ
                && v.getY() >= minY - FLOOR_ABOVE_LOWEST - TARGET_VERTICAL_SLACK
                && v.getY() <= maxY + TARGET_VERTICAL_SLACK;
    }

    public Vector patrolPoint(double angle) {
        return clamp(new Vector(
                center.getX() + Math.cos(angle) * patrolRadius,
                center.getY(),
                center.getZ() + Math.sin(angle) * patrolRadius));
    }

    public double angleOf(Vector v) {
        return Math.atan2(v.getZ() - center.getZ(), v.getX() - center.getX());
    }

    public Vector center() {
        return center.clone();
    }

    public double patrolY() {
        return center.getY();
    }

    public double patrolRadius() {
        return patrolRadius;
    }

    public double minY() {
        return minY;
    }

    public double maxY() {
        return maxY;
    }
}
