package com.l299l.newbedwars.arena.generators.leveling;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class GeneratorLeveling {
    private final String id;
    private final GeneratorSettings basicGeneratorSettings;
    private final GeneratorSettings diamondGeneratorSettings;
    private final GeneratorSettings emeraldGeneratorSettings;
    private final Map<String, CustomGeneratorSettings> customGenerators;

    public GeneratorLeveling(FileConfiguration configuration) {
        this(configuration, com.l299l.newbedwars.NewBedwars.plugin != null
                ? com.l299l.newbedwars.NewBedwars.plugin.getLogger() : Logger.getLogger("NewBedwars"));
    }

    public GeneratorLeveling(FileConfiguration configuration, Logger log) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        id = configuration.getString("ConfigurationId");
        basicGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("TeamGenerator"));
        diamondGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("DiamondGenerator"));
        emeraldGeneratorSettings = new GeneratorSettings(configuration.getConfigurationSection("EmeraldGenerator"));
        customGenerators = CustomGeneratorSettings.read(configuration.getConfigurationSection("CustomGenerators"), log);
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

    public CustomGeneratorSettings getCustomGenerator(String id) {
        return id == null ? null : customGenerators.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, CustomGeneratorSettings> getCustomGenerators() {
        return customGenerators;
    }
}
