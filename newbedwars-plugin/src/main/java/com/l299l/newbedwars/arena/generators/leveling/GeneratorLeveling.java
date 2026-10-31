package com.l299l.newbedwars.arena.generators.leveling;

import org.bukkit.configuration.file.FileConfiguration;

public class GeneratorLeveling {
    private final String id;
    private final GeneratorSettings basicGeneratorSettings;
    private final GeneratorSettings diamondGeneratorSettings;
    private final GeneratorSettings emeraldGeneratorSettings;

    public GeneratorLeveling(FileConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        id = configuration.getString("ConfigurationId");
        basicGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("TeamGenerator"));
        diamondGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("DiamondGenerator"));
        emeraldGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("EmeraldGenerator"));
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
}
