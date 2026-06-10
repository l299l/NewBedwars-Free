package com.l299l.newbedwars.arena.gamerules;

public class SpecialGamerule {
    private final String name;
    private final String description;

    public SpecialGamerule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static SpecialGamerule getByName(String name) {
        return null;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }
}
