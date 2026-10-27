package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.GeneratorKind;
import com.l299l.newbedwars.api.GeneratorView;
import com.l299l.newbedwars.arena.generators.Generator;
import org.bukkit.Location;

final class ApiGenerator implements GeneratorView {
    private final Generator generator;

    ApiGenerator(Generator generator) {
        this.generator = generator;
    }

    @Override
    public GeneratorKind getKind() {
        return switch (generator.getType()) {
            case DIAMOND -> GeneratorKind.DIAMOND;
            case EMERALD -> GeneratorKind.EMERALD;
            default -> GeneratorKind.BASIC;
        };
    }

    @Override
    public Location getLocation() {
        return generator.getLocation().clone();
    }
}
