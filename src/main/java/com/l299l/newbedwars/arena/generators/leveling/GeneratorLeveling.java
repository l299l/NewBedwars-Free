package com.l299l.newbedwars.arena.generators.leveling;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;

public class GeneratorLeveling {
    private final String id;
    private final GeneratorSettings basicGeneratorSettings;
    private final GeneratorSettings diamondGeneratorSettings;
    private final GeneratorSettings emeraldGeneratorSettings;
    private final HashMap<String, GeneratorSettings> otherGeneratorSettings;

    public GeneratorLeveling(FileConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        id = configuration.getString("ConfigurationId");
        basicGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("TeamGenerator"));
        diamondGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("DiamondGenerator"));
        emeraldGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("EmeraldGenerator"));
        otherGeneratorSettings = new HashMap<>();
        boolean otherGeneratorsEnabled = configuration.getBoolean("OtherGenerators.Enabled");
    }

    public String getId() {
        return id;
    }

    public GeneratorSettings getDefaultGeneratorSettings() {
        return basicGeneratorSettings;
    }

    public GeneratorSettings getDiamondGeneratorSettings() {
        return diamondGeneratorSettings;
    }

    public GeneratorSettings getEmeraldGeneratorSettings() {
        return emeraldGeneratorSettings;
    }

    public GeneratorSettings getOtherGeneratorSettings(String name) {
        return otherGeneratorSettings.get(name);
    }
}
