package com.l299l.newbedwars.panel.dashboard;

import com.l299l.newbedwars.api.mode.ArenaModeInfo;
import com.l299l.newbedwars.api.mode.ModeInfo;
import com.l299l.newbedwars.api.view.ArenaView;

import java.util.List;
import java.util.Objects;

public final class ModeMatch {

    private ModeMatch() {}

    public static String of(ArenaView arena, List<ModeInfo> modes) {
        if (arena == null || modes == null) return null;
        return of(arena.getGeneratorConfig(), arena.getGamePhases(), arena.getShopGui(), arena.getUpgradeGui(),
                arena.getMaxInTeam(), modes);
    }

    public static String of(ArenaModeInfo arena, List<ModeInfo> modes) {
        if (arena == null || modes == null) return null;
        return of(arena.generators(), arena.gamePhases(), arena.shopGui(), arena.upgradeGui(), arena.maxInTeam(), modes);
    }

    static String of(String generators, String phases, String shopGui, String upgradeGui, int maxInTeam, List<ModeInfo> modes) {
        for (ModeInfo mode : modes) {
            if (!sets(mode)) continue;
            if (mode.generators() != null && !Objects.equals(mode.generators(), generators)) continue;
            if (mode.gamePhases() != null && !Objects.equals(mode.gamePhases(), phases)) continue;
            if (mode.shopGui() != null && !Objects.equals(mode.shopGui(), shopGui)) continue;
            if (mode.upgradeGui() != null && !Objects.equals(mode.upgradeGui(), upgradeGui)) continue;
            if (mode.maxInTeam() > 0 && mode.maxInTeam() != maxInTeam) continue;
            return mode.id();
        }
        return null;
    }

    private static boolean sets(ModeInfo mode) {
        return mode.generators() != null || mode.gamePhases() != null || mode.shopGui() != null
                || mode.upgradeGui() != null || mode.maxInTeam() > 0;
    }
}
