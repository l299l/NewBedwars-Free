package com.l299l.newbedwars.arena.generators.leveling;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorSettingsTest {

    private static YamlConfiguration config(String yaml) throws Exception {
        YamlConfiguration conf = new YamlConfiguration();
        conf.loadFromString(yaml);
        return conf;
    }

    @Test
    void singleSpawnDelayIsExpandedToEveryLevel() throws Exception {
        GeneratorSettings settings = new GeneratorSettings(config("""
                MaxLevel: 3
                ShowNextSpawnTime: false
                Iron:
                  SpawnRate: [100.0]
                  MaxAmount: [2]
                SpawnDelay: [30]
                """));
        assertEquals(3, settings.getSpawnDelay().size());
        assertEquals(30.0, settings.getSpawnDelay().get(2));
    }

    @Test
    void spawnDelayListOfWrongSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GeneratorSettings(config("""
                MaxLevel: 3
                ShowNextSpawnTime: false
                Iron:
                  SpawnRate: [100.0]
                  MaxAmount: [2]
                SpawnDelay: [30, 20]
                """)));
    }

    // ── shipped defaults ────────────────────────────────────────────────────

    private static GeneratorSettings shipped(String section) {
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(new java.io.File("src/main/resources/defaultGenerators.yml"));
        return new GeneratorSettings(conf.getConfigurationSection(section));
    }

    /** Items per second of one resource at a level: share of the weights x average amount / delay. */
    private static double income(GeneratorSettings s, int level, java.util.function.Function<GeneratorSettings, java.util.List<Double>> rate,
                                 java.util.function.Function<GeneratorSettings, java.util.List<Integer>> amount) {
        double total = 0;
        for (var r : java.util.List.of(s.getIronSpawnRate(), s.getGoldSpawnRate(), s.getEmeraldSpawnRate(), s.getDiamondSpawnRate())) {
            if (r != null) total += r.get(level);
        }
        double share = rate.apply(s).get(level) / total;
        double avg = (amount.apply(s).get(level) + 1) / 2.0;
        return share * avg / s.getSpawnDelay().get(level);
    }

    @Test
    void defaultTeamGeneratorHasOneLevelPerForgePurchase() {
        GeneratorSettings s = shipped("TeamGenerator");
        assertEquals(9, s.getMaxLevel()); // base level + the 8 Forge levels of config.yml
    }

    @Test
    void defaultTeamGeneratorIncomeGrowsWithEveryLevel() {
        GeneratorSettings s = shipped("TeamGenerator");
        double lastIron = 0, lastGold = 0;
        for (int level = 0; level < s.getMaxLevel(); level++) {
            double iron = income(s, level, GeneratorSettings::getIronSpawnRate, GeneratorSettings::getIronSpawnAmount);
            double gold = income(s, level, GeneratorSettings::getGoldSpawnRate, GeneratorSettings::getGoldSpawnAmount);
            assertTrue(iron > lastIron, "iron must rise at level " + (level + 1));
            assertTrue(gold > lastGold, "gold must rise at level " + (level + 1));
            assertTrue(iron > gold * 4, "gold stays much rarer than iron at level " + (level + 1));
            lastIron = iron;
            lastGold = gold;
        }
    }

    @Test
    void defaultTeamGeneratorIronStartsNearOnePerSecondAndTriplesAtTheEnd() {
        GeneratorSettings s = shipped("TeamGenerator");
        double base = income(s, 0, GeneratorSettings::getIronSpawnRate, GeneratorSettings::getIronSpawnAmount);
        double max = income(s, s.getMaxLevel() - 1, GeneratorSettings::getIronSpawnRate, GeneratorSettings::getIronSpawnAmount);
        assertEquals(1.0, base, 0.1);
        assertEquals(3.0, max, 0.3);
    }

    @Test
    void defaultTeamGeneratorUnlocksEmeraldsAtLevelFourAndDiamondsAtLevelSix() {
        GeneratorSettings s = shipped("TeamGenerator");
        for (int level = 0; level < 3; level++) {
            assertEquals(0.0, s.getEmeraldSpawnRate().get(level));
        }
        assertTrue(s.getEmeraldSpawnRate().get(3) > 0);
        assertEquals(0.0, s.getDiamondSpawnRate().get(4));
        assertTrue(s.getDiamondSpawnRate().get(5) > 0);
    }

    @Test
    void islandGeneratorsKeepTheHypixelIntervals() {
        assertEquals(java.util.List.of(30.0, 24.0, 12.0), shipped("DiamondGenerator").getSpawnDelay());
        assertEquals(java.util.List.of(56.0, 40.0, 28.0), shipped("EmeraldGenerator").getSpawnDelay());
    }
}
