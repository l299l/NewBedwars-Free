package com.l299l.newbedwars.panel.dashboard;

import com.l299l.newbedwars.api.mode.ArenaModeInfo;
import com.l299l.newbedwars.api.mode.ModeInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModeMatchTest {
    private static ModeInfo mode(String id, String gen, String phases, String shop, String upgrade, int size) {
        return new ModeInfo(id, gen, phases, shop, upgrade, size, 0, false);
    }

    @Test
    void exactMatch() {
        List<ModeInfo> modes = List.of(mode("Squads", "fast", "quick", "shop", "ups", 4));
        assertEquals("Squads", ModeMatch.of("fast", "quick", "shop", "ups", 4, modes));
    }

    @Test
    void nullPartsDoNotConstrain() {
        List<ModeInfo> modes = List.of(mode("Fast", "fast", null, null, null, 0));
        assertEquals("Fast", ModeMatch.of("fast", "anything", "other", null, 7, modes));
    }

    @Test
    void zeroMaxInTeamDoesNotConstrain() {
        List<ModeInfo> modes = List.of(mode("Fast", "fast", "quick", null, null, 0));
        assertEquals("Fast", ModeMatch.of("fast", "quick", null, null, 8, modes));
    }

    @Test
    void mismatchingMaxInTeamDoesNotMatch() {
        List<ModeInfo> modes = List.of(mode("Duos", "fast", null, null, null, 2));
        assertNull(ModeMatch.of("fast", "quick", null, null, 4, modes));
    }

    @Test
    void firstMatchWins() {
        List<ModeInfo> modes = List.of(mode("A", "fast", null, null, null, 0), mode("B", "fast", "quick", null, null, 0));
        assertEquals("A", ModeMatch.of("fast", "quick", null, null, 4, modes));
    }

    @Test
    void modeThatSetsNothingNeverMatches() {
        List<ModeInfo> modes = List.of(mode("Empty", null, null, null, null, 0));
        assertNull(ModeMatch.of("fast", "quick", "shop", "ups", 4, modes));
    }

    @Test
    void noMatchIsNull() {
        List<ModeInfo> modes = List.of(mode("Squads", "fast", "quick", null, null, 4));
        assertNull(ModeMatch.of("slow", "quick", null, null, 4, modes));
        assertNull(ModeMatch.of("fast", "quick", null, null, 4, List.of()));
    }

    @Test
    void arenaModeInfoOverload() {
        List<ModeInfo> modes = List.of(mode("Squads", "fast", "quick", null, null, 4));
        assertEquals("Squads", ModeMatch.of(new ArenaModeInfo("a", true, "fast", "quick", "x", "y", 4, 4), modes));
        assertNull(ModeMatch.of(new ArenaModeInfo("a", true, "slow", "quick", "x", "y", 4, 4), modes));
        assertNull(ModeMatch.of((ArenaModeInfo) null, modes));
    }
}
