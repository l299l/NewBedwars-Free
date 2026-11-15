package com.l299l.newbedwars.arena.setup.auto;

import java.util.ArrayList;
import java.util.List;

public final class AreaGuesser {
    public static final int PROTECTION_PADDING = 3;

    public record Padding(int sides, int below, int above) {}

    private AreaGuesser() {}

    public static Box base(List<Pos> points, Padding padding) {
        Box box = bounds(points);
        return box == null ? null : box.grow(padding.sides(), padding.below(), padding.above());
    }

    public static Box buildProtection(Pos spawn, Pos generator, List<Pos> fallback) {
        List<Pos> core = new ArrayList<>();
        if (spawn != null) core.add(spawn);
        if (generator != null) core.add(generator);
        Box box = bounds(core.isEmpty() ? fallback : core);
        return box == null ? null : box.grow(PROTECTION_PADDING, PROTECTION_PADDING, PROTECTION_PADDING);
    }

    private static Box bounds(List<Pos> points) {
        Box box = null;
        for (Pos point : points) {
            if (point == null) continue;
            box = box == null ? Box.of(point, point) : Box.of(
                    new Pos(Math.min(box.min().x(), point.x()), Math.min(box.min().y(), point.y()), Math.min(box.min().z(), point.z())),
                    new Pos(Math.max(box.max().x(), point.x()), Math.max(box.max().y(), point.y()), Math.max(box.max().z(), point.z())));
        }
        return box;
    }
}
