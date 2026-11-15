package com.l299l.newbedwars.arena.setup.auto;

public record Pos(int x, int y, int z) {

    public long distanceSquared(Pos other) {
        long dx = x - other.x;
        long dy = y - other.y;
        long dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Pos above() {
        return new Pos(x, y + 1, z);
    }
}
