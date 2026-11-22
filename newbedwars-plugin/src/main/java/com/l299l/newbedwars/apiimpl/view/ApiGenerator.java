package com.l299l.newbedwars.apiimpl.view;

import com.l299l.newbedwars.api.view.GeneratorKind;
import com.l299l.newbedwars.api.view.GeneratorView;
import com.l299l.newbedwars.arena.generators.Generator;
import org.bukkit.Location;

public final class ApiGenerator implements GeneratorView {
    private final Generator generator;

    ApiGenerator(Generator generator) {
        this.generator = generator;
    }

    @Override
    public GeneratorKind getKind() {
        return switch (generator.getType()) {
            case DIAMOND -> GeneratorKind.DIAMOND;
            case EMERALD -> GeneratorKind.EMERALD;
            case OTHER -> GeneratorKind.CUSTOM;
            default -> GeneratorKind.BASIC;
        };
    }

    @Override
    public String getCustomId() {
        return generator instanceof com.l299l.newbedwars.arena.generators.CustomGenerator custom ? custom.getId() : null;
    }

    @Override
    public Location getLocation() {
        return generator.getLocation().clone();
    }
}
