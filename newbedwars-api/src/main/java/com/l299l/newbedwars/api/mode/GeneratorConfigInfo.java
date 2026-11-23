package com.l299l.newbedwars.api.mode;

/**
 * A generator config file in {@code plugins/NewBedwars/generators/}.
 *
 * @param id       the {@code ConfigurationId} inside the file
 * @param fileName file name inside the generators folder, e.g. {@code Solo.yml}
 * @param builtIn  {@code true} for a config that ships with NewBedwars
 * @since 2.8-beta
 */
public record GeneratorConfigInfo(String id, String fileName, boolean builtIn) {
}
