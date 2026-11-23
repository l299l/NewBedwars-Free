package com.l299l.newbedwars.arena.modes;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.config.properties.GameMode;
import com.l299l.newbedwars.config.properties.Properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModeApplier {

    public record Result(boolean applied, List<String> missing, boolean teamsDiffer, int arenaTeams) {
    }

    private ModeApplier() {}

    public static GameMode find(String id) {
        if (id == null) return null;
        GameMode exact = Properties.Modes.get(id);
        if (exact != null) return exact;
        for (Map.Entry<String, GameMode> e : Properties.Modes.entrySet()) {
            if (e.getKey().equalsIgnoreCase(id)) return e.getValue();
        }
        return null;
    }

    public static Result apply(IArena arena, GameMode mode) {
        GeneratorLeveling leveling = mode.generators() == null ? null : NewBedwars.plugin.getGeneratorLeveling().get(mode.generators());
        GamePhases phases = mode.gamePhases() == null ? null : Properties.GamePhases.get(mode.gamePhases());
        List<String> missing = new ArrayList<>();
        if (mode.generators() != null && leveling == null) missing.add(mode.generators());
        if (mode.gamePhases() != null && phases == null) missing.add(mode.gamePhases());
        if (mode.shopGui() != null && NewBedwars.plugin.getGuiManager().getGui(mode.shopGui()) == null) missing.add(mode.shopGui());
        if (mode.upgradeGui() != null && NewBedwars.plugin.getGuiManager().getGui(mode.upgradeGui()) == null) missing.add(mode.upgradeGui());
        int teams = arena.getTeams().size();
        if (!missing.isEmpty()) return new Result(false, List.copyOf(missing), false, teams);
        if (leveling != null) arena.setGeneratorsLeveling(leveling);
        if (phases != null) arena.setGamePhases(phases);
        if (mode.shopGui() != null) arena.setShopGuiId(mode.shopGui());
        if (mode.upgradeGui() != null) arena.setUpgradeGuiId(mode.upgradeGui());
        if (mode.maxInTeam() > 0) arena.setMaxInTeam(mode.maxInTeam());
        new ArenaDataJson(arena).save();
        return new Result(true, List.of(), mode.teams() > 0 && teams != mode.teams(), teams);
    }
}
