package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.mode.ArenaModeInfo;
import com.l299l.newbedwars.api.mode.ModeInfo;
import com.l299l.newbedwars.api.mode.ModeResult;
import com.l299l.newbedwars.api.mode.ModeService;
import com.l299l.newbedwars.api.mode.PhaseInfo;
import com.l299l.newbedwars.api.mode.PhaseListInfo;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.modes.ModeApplier;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.phases.Phase;
import com.l299l.newbedwars.arena.phases.PhaseAction;
import com.l299l.newbedwars.config.ModesFile;
import com.l299l.newbedwars.config.properties.GameMode;
import com.l299l.newbedwars.config.properties.Properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ApiModeService implements ModeService {

    private static <T> CompletableFuture<T> run(Supplier<T> action) {
        return ApiMaintenance.onMain(() -> CompletableFuture.completedFuture(action.get()));
    }

    @Override
    public CompletableFuture<List<ModeInfo>> getModes() {
        return run(ApiModeService::modes);
    }

    @Override
    public CompletableFuture<List<PhaseListInfo>> getPhaseLists() {
        return run(ApiModeService::phaseLists);
    }

    @Override
    public CompletableFuture<List<ArenaModeInfo>> getArenaModes() {
        return run(ApiModeService::arenaModes);
    }

    @Override
    public CompletableFuture<List<String>> getGeneratorConfigs() {
        return run(() -> new ArrayList<>(new TreeSet<>(NewBedwars.plugin.getGeneratorLeveling().keySet())));
    }

    @Override
    public CompletableFuture<ModeResult> applyMode(String arena, String mode) {
        return run(() -> apply(arena, mode));
    }

    static List<ModeInfo> modes() {
        Object builtIn = ModesFile.bundled().get(ModesFile.MODES);
        List<ModeInfo> out = new ArrayList<>();
        for (GameMode m : Properties.Modes.values()) {
            boolean bundled = builtIn instanceof Map<?, ?> map && map.containsKey(m.id());
            out.add(new ModeInfo(m.id(), m.generators(), m.gamePhases(), m.shopGui(), m.upgradeGui(),
                    m.maxInTeam(), m.teams(), bundled));
        }
        return out;
    }

    static List<ArenaModeInfo> arenaModes() {
        List<ArenaModeInfo> out = new ArrayList<>();
        for (IArena arena : ArenaRegistry.all()) {
            out.add(new ArenaModeInfo(arena.getArenaName(), arena.status() == GameStatus.waiting,
                    arena.getGeneratorsLevelingId(), arena.getGamePhasesId(), arena.getShopGuiId(), arena.getUpgradeGuiId(),
                    arena.getMaxInTeam(), arena.getTeams().size()));
        }
        out.sort(java.util.Comparator.comparing(ArenaModeInfo::arena, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    static List<PhaseListInfo> phaseLists() {
        List<PhaseListInfo> out = new ArrayList<>();
        for (String id : new TreeSet<>(Properties.GamePhases.keySet())) {
            GamePhases list = Properties.GamePhases.get(id);
            List<PhaseInfo> phases = new ArrayList<>();
            for (Phase phase : list.getPhases()) {
                List<String> actions = new ArrayList<>();
                for (PhaseAction action : phase.getActions()) actions.add(action.action());
                phases.add(new PhaseInfo(phase.getName(), phase.getInitialDuration(), actions));
            }
            out.add(new PhaseListInfo(id, phases));
        }
        return out;
    }

    static ModeResult apply(String arenaName, String modeName) {
        IArena arena = arenaName == null ? null : ArenaRegistry.find(arenaName);
        GameMode mode = ModeApplier.find(modeName);
        if (arena == null || mode == null) return new ModeResult(ModeResult.Code.NOT_FOUND, List.of(), false);
        if (arena.status() != GameStatus.waiting) return new ModeResult(ModeResult.Code.BUSY, List.of(), false);
        ModeApplier.Result result = ModeApplier.apply(arena, mode);
        if (!result.applied()) return new ModeResult(ModeResult.Code.MISSING_PARTS, result.missing(), false);
        return new ModeResult(ModeResult.Code.OK, List.of(), result.teamsDiffer());
    }
}
