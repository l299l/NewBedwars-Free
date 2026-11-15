package com.l299l.newbedwars.arena.setup.auto;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanInterpreterTest {
    private static final ScanInterpreter.Settings SETTINGS = new ScanInterpreter.Settings(20, -64);

    private static ScanResult run(ScanData data) {
        return ScanInterpreter.interpret(data, SETTINGS);
    }

    private static void bed(ScanData data, ChatColor color, int x, int y, int z) {
        data.beds().add(new ScanData.Bed(new Pos(x, y, z), color));
    }

    private static void block(ScanData data, ScanData.Block type, int x, int y, int z) {
        data.blocks().add(new ScanData.GenBlock(new Pos(x, y, z), type));
    }

    private static void sign(ScanData data, SignMark.Kind kind, String team, String extra, int x, int y, int z) {
        data.signs().add(new SignMark(new Pos(x, y, z), kind, team, extra));
    }

    @Test
    void everyBedColorBecomesATeam() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 100, 64, 0);
        bed(data, ChatColor.DARK_BLUE, -100, 64, 0);
        ScanResult result = run(data);
        assertEquals(2, result.teams.size());
        assertEquals("Red", result.team(ChatColor.RED).name);
        assertEquals("Blue", result.team(ChatColor.DARK_BLUE).name);
    }

    @Test
    void secondBedOfTheSameColorIsIgnoredWithAWarning() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 100, 64, 0);
        bed(data, ChatColor.RED, 100, 64, 50);
        ScanResult result = run(data);
        assertEquals(1, result.teams.size());
        assertEquals(new Pos(100, 64, 0), result.teams.get(0).bed);
        assertEquals("AutoSetup-WarnDuplicateBed", result.warnings.get(0).key());
    }

    @Test
    void signWithTeamColorGoesToThatTeam() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 100, 64, 0);
        bed(data, ChatColor.DARK_BLUE, -100, 64, 0);
        sign(data, SignMark.Kind.SPAWN, "blue", "", 100, 64, 3);
        ScanResult result = run(data);
        assertEquals(new Pos(100, 64, 3), result.team(ChatColor.DARK_BLUE).spawn);
        assertNull(result.team(ChatColor.RED).spawn);
    }

    @Test
    void signWithoutTeamGoesToTheNearestBed() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 100, 64, 0);
        bed(data, ChatColor.DARK_BLUE, -100, 64, 0);
        sign(data, SignMark.Kind.SHOP, "", "CREEPER", -95, 64, 2);
        sign(data, SignMark.Kind.UPGRADES, "", "", 97, 64, 2);
        ScanResult result = run(data);
        assertEquals(new Pos(-95, 64, 2), result.team(ChatColor.DARK_BLUE).shop);
        assertEquals("CREEPER", result.team(ChatColor.DARK_BLUE).shopEntity);
        assertEquals(new Pos(97, 64, 2), result.team(ChatColor.RED).upgrades);
        assertEquals(2, result.usedSigns.size());
    }

    @Test
    void signForATeamWithoutBedIsReported() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 0, 64, 0);
        sign(data, SignMark.Kind.SPAWN, "yellow", "", 1, 64, 1);
        sign(data, SignMark.Kind.SPAWN, "notacolor", "", 2, 64, 2);
        ScanResult result = run(data);
        assertNull(result.team(ChatColor.RED).spawn);
        assertEquals(0, result.usedSigns.size());
        assertEquals("AutoSetup-WarnSignTeamMissing", result.warnings.get(0).key());
        assertEquals("AutoSetup-WarnBadSignTeam", result.warnings.get(1).key());
    }

    @Test
    void secondSignOfTheSameKindIsIgnored() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 0, 64, 0);
        sign(data, SignMark.Kind.SPAWN, "", "", 1, 64, 0);
        sign(data, SignMark.Kind.SPAWN, "", "", 5, 64, 0);
        ScanResult result = run(data);
        assertEquals(new Pos(1, 64, 0), result.team(ChatColor.RED).spawn);
        assertEquals(1, result.usedSigns.size());
        assertEquals("AutoSetup-WarnDuplicateSign", result.warnings.get(0).key());
    }

    @Test
    void nearestIronOrGoldBlockBecomesTheTeamGeneratorOneBlockUp() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 100, 64, 0);
        bed(data, ChatColor.DARK_BLUE, -100, 64, 0);
        block(data, ScanData.Block.IRON, 105, 63, 0);
        block(data, ScanData.Block.GOLD, 110, 63, 0);
        block(data, ScanData.Block.IRON, -103, 63, 0);
        ScanResult result = run(data);
        assertEquals(new Pos(105, 64, 0), result.team(ChatColor.RED).generator);
        assertEquals(new Pos(-103, 64, 0), result.team(ChatColor.DARK_BLUE).generator);
    }

    @Test
    void generatorBlockOutOfRangeIsNotUsed() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 0, 64, 0);
        block(data, ScanData.Block.IRON, 50, 63, 0);
        assertNull(run(data).team(ChatColor.RED).generator);
    }

    @Test
    void oneBlockIsNotGivenToTwoTeams() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 0, 64, 0);
        bed(data, ChatColor.DARK_BLUE, 10, 64, 0);
        block(data, ScanData.Block.IRON, 4, 63, 0);
        ScanResult result = run(data);
        assertNotNull(result.team(ChatColor.RED).generator);
        assertNull(result.team(ChatColor.DARK_BLUE).generator);
    }

    @Test
    void generatorSignWinsOverBlocks() {
        ScanData data = new ScanData();
        bed(data, ChatColor.RED, 0, 64, 0);
        block(data, ScanData.Block.IRON, 4, 63, 0);
        sign(data, SignMark.Kind.GENERATOR, "red", "", 8, 64, 0);
        assertEquals(new Pos(8, 64, 0), run(data).team(ChatColor.RED).generator);
    }

    @Test
    void diamondAndEmeraldBlocksBecomeArenaGeneratorsOneBlockUp() {
        ScanData data = new ScanData();
        block(data, ScanData.Block.DIAMOND, 0, 60, 0);
        block(data, ScanData.Block.EMERALD, 50, 70, 50);
        ScanResult result = run(data);
        assertEquals(java.util.List.of(new Pos(0, 61, 0)), result.diamondGenerators);
        assertEquals(java.util.List.of(new Pos(50, 71, 50)), result.emeraldGenerators);
    }

    @Test
    void touchingBlocksMakeOneGenerator() {
        ScanData data = new ScanData();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) block(data, ScanData.Block.EMERALD, x, 60, z);
        }
        ScanResult result = run(data);
        assertEquals(java.util.List.of(new Pos(1, 61, 1)), result.emeraldGenerators);
    }

    @Test
    void markedGeneratorSignsAddArenaGenerators() {
        ScanData data = new ScanData();
        sign(data, SignMark.Kind.GENERATOR, "diamond", "", 10, 64, 10);
        sign(data, SignMark.Kind.GENERATOR, "EMERALD", "", -10, 64, -10);
        ScanResult result = run(data);
        assertEquals(java.util.List.of(new Pos(10, 64, 10)), result.diamondGenerators);
        assertEquals(java.util.List.of(new Pos(-10, 64, -10)), result.emeraldGenerators);
        assertEquals(2, result.usedSigns.size());
    }

    @Test
    void quickVoidIsTenBelowTheLowestBlockButNotBelowTheWorld() {
        ScanData data = new ScanData();
        data.solidAt(40);
        data.solidAt(30);
        assertEquals(20, run(data).quickVoidY);
        ScanData deep = new ScanData();
        deep.solidAt(-60);
        assertEquals(-64, run(deep).quickVoidY);
        assertNull(run(new ScanData()).quickVoidY);
    }

    @Test
    void noBedsGivesAWarning() {
        ScanResult result = run(new ScanData());
        assertTrue(result.teams.isEmpty());
        assertEquals("AutoSetup-WarnNoBeds", result.warnings.get(0).key());
    }
}
