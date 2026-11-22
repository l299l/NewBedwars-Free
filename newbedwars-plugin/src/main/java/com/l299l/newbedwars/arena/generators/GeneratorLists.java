package com.l299l.newbedwars.arena.generators;

import java.util.List;

final class GeneratorLists {
    private GeneratorLists() {
    }

    static double doubleAt(List<Double> values, int index, double fallback) {
        if (values == null || values.isEmpty() || index < 0) return fallback;
        Double value = values.get(Math.min(index, values.size() - 1));
        return value == null ? fallback : value;
    }

    static int intAt(List<Integer> values, int index, int fallback) {
        if (values == null || values.isEmpty() || index < 0) return fallback;
        Integer value = values.get(Math.min(index, values.size() - 1));
        return value == null ? fallback : value;
    }
}
