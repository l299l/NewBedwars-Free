package com.l299l.newbedwars.config;

import java.util.List;

public final class BuiltInModes {
    public static final List<String> NAMES = List.of("Solo", "Doubles", "Squads", "Rush", "Duels");

    private BuiltInModes() {}

    public static String generatorResource(String mode) {
        return "generators/" + mode + ".yml";
    }

    public static String upgradeGuiFile(String mode) {
        return "upgrade" + mode + ".yml";
    }
}
