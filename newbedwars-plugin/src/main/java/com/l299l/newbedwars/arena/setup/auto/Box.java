package com.l299l.newbedwars.arena.setup.auto;

public record Box(Pos min, Pos max) {

    public static Box of(Pos a, Pos b) {
        return new Box(new Pos(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
                new Pos(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())));
    }

    public Box grow(int sides, int below, int above) {
        return new Box(new Pos(min.x() - sides, min.y() - below, min.z() - sides),
                new Pos(max.x() + sides, max.y() + above, max.z() + sides));
    }
}
