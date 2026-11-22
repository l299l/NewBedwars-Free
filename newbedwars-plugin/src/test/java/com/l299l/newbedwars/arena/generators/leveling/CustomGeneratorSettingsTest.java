package com.l299l.newbedwars.arena.generators.leveling;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomGeneratorSettingsTest {

    @Test
    void readsDefinitionsAndSkipsUnknownItems() throws Exception {
        YamlConfiguration conf = new YamlConfiguration();
        conf.loadFromString("""
                CustomGenerators:
                  Gapple:
                    Item: GOLDEN_APPLE
                    Amount: 2
                    SpawnDelay: 45
                    Display: GOLD_BLOCK
                    Name: "&6Apples"
                    ShowNextSpawnTime: false
                  Pearl:
                    Item: ender_pearl
                    Amount: 500
                    SpawnDelay: 0
                  Broken:
                    Item: NOT_AN_ITEM
                """);
        Map<String, CustomGeneratorSettings> read = CustomGeneratorSettings.read(conf.getConfigurationSection("CustomGenerators"),
                Logger.getLogger("test"));
        assertEquals(2, read.size());
        CustomGeneratorSettings gapple = read.get("gapple");
        assertEquals("Gapple", gapple.id());
        assertEquals(Material.GOLDEN_APPLE, gapple.item());
        assertEquals(2, gapple.amount());
        assertEquals(45, gapple.spawnDelay());
        assertEquals(Material.GOLD_BLOCK, gapple.display());
        assertEquals("&6Apples", gapple.name());
        assertFalse(gapple.showNextSpawnTime());
        CustomGeneratorSettings pearl = read.get("pearl");
        assertEquals(64, pearl.amount());
        assertEquals(1, pearl.spawnDelay());
        assertEquals(Material.ENDER_PEARL, pearl.display());
        assertEquals("Ender Pearl", pearl.name());
        assertTrue(pearl.showNextSpawnTime());
    }

    @Test
    void missingSectionMeansNone() {
        assertTrue(CustomGeneratorSettings.read(null, Logger.getLogger("test")).isEmpty());
    }
}
