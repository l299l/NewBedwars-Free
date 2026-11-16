package com.l299l.newbedwars.arena.generators;

import java.util.ArrayList;
import java.util.List;

public final class GeneratorHologramText {
    static final String DELAY_PLACEHOLDER = "/delay/";

    private GeneratorHologramText() {}

    public static String of(String text, boolean showNextSpawnTime) {
        if (showNextSpawnTime || text == null) return text;
        List<String> kept = new ArrayList<>();
        for (String line : text.split(";|\n")) {
            if (!line.contains(DELAY_PLACEHOLDER)) kept.add(line);
        }
        return kept.isEmpty() ? text : String.join(";", kept);
    }
}
