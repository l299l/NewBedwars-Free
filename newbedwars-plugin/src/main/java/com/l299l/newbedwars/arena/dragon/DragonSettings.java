package com.l299l.newbedwars.arena.dragon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record DragonSettings(double speed, double turnRate, double patrolHeight, double patrolRadius, double margin,
                             double targetRange, double attackRange, List<Integer> fireballIntervalSeconds,
                             double fireballSpeed) {

    public static final DragonSettings DEFAULTS = new DragonSettings(0.6, 6.0, 15, 0, 10, 128, 20, List.of(20, 10), 1.0);

    public DragonSettings {
        fireballIntervalSeconds = List.copyOf(fireballIntervalSeconds);
    }

    public static DragonSettings read(Function<String, Object> get) {
        return new DragonSettings(
                number(get.apply("Dragon.Speed"), DEFAULTS.speed, 0.05, 5),
                number(get.apply("Dragon.TurnRate"), DEFAULTS.turnRate, 0.5, 180),
                number(get.apply("Dragon.PatrolHeight"), DEFAULTS.patrolHeight, 0, 200),
                number(get.apply("Dragon.PatrolRadius"), DEFAULTS.patrolRadius, 0, 1000),
                number(get.apply("Dragon.Margin"), DEFAULTS.margin, 0, 200),
                number(get.apply("Dragon.TargetRange"), DEFAULTS.targetRange, 1, 1000),
                number(get.apply("Dragon.AttackRange"), DEFAULTS.attackRange, 1, 200),
                intervals(get.apply("Dragon.FireballIntervalSeconds")),
                number(get.apply("Dragon.FireballSpeed"), DEFAULTS.fireballSpeed, 0.1, 5));
    }

    public int fireballIntervalTicks(int level) {
        int index = Math.min(Math.max(level, 1), fireballIntervalSeconds.size()) - 1;
        return fireballIntervalSeconds.get(index) * 20;
    }

    private static double number(Object raw, double fallback, double min, double max) {
        if (!(raw instanceof Number n)) return fallback;
        double value = n.doubleValue();
        if (Double.isNaN(value) || value < min || value > max) return fallback;
        return value;
    }

    private static List<Integer> intervals(Object raw) {
        if (!(raw instanceof List<?> list)) return DEFAULTS.fireballIntervalSeconds;
        List<Integer> out = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof Number n && n.intValue() > 0) out.add(n.intValue());
        }
        return out.isEmpty() ? DEFAULTS.fireballIntervalSeconds : out;
    }
}
