package com.l299l.newbedwars.arena.generators;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneratorLevelingSwitchTest {
    private World world;
    private IArena arena;
    private Location location;

    static GeneratorLeveling leveling(String id, int teamLevels, int diamondLevels) throws InvalidConfigurationException {
        YamlConfiguration conf = new YamlConfiguration();
        conf.loadFromString("ConfigurationId: " + id + "\n"
                + "TeamGenerator:\n  MaxLevel: " + teamLevels + "\n  Iron:\n    SpawnRate: [100.0]\n    MaxAmount: [1]\n  SpawnDelay: [1.0]\n"
                + "DiamondGenerator:\n  MaxLevel: " + diamondLevels + "\n  Diamond:\n    SpawnRate: [100.0]\n    MaxAmount: [1]\n  SpawnDelay: [30]\n"
                + "EmeraldGenerator:\n  MaxLevel: 1\n  Emerald:\n    SpawnRate: [100.0]\n    MaxAmount: [1]\n  SpawnDelay: [60]\n");
        return new GeneratorLeveling(conf);
    }

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        arena = mock(IArena.class);
        ArenaRegistry.bindWorld(world, arena);
        location = new Location(world, 0, 64, 0);
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.unbindWorld(world);
    }

    @Test
    void basicGeneratorFollowsTheArenaConfigChosenAfterItWasBuilt() throws Exception {
        when(arena.getGeneratorsLeveling()).thenReturn(leveling("Default", 3, 3));
        BasicGenerator generator = new BasicGenerator(location);
        when(arena.getGeneratorsLeveling()).thenReturn(leveling("Big", 12, 3));

        assertEquals(12, generator.getMaxLevel());
        assertTrue(generator.upgrade(12));
        assertEquals(12, generator.getLevel());
    }

    @Test
    void basicGeneratorWithAShorterConfigStopsAtItsMaxInsteadOfThrowing() throws Exception {
        when(arena.getGeneratorsLeveling()).thenReturn(leveling("Default", 9, 3));
        BasicGenerator generator = new BasicGenerator(location);
        when(arena.getGeneratorsLeveling()).thenReturn(leveling("Small", 2, 3));

        assertDoesNotThrow(() -> generator.upgrade(9));
        assertEquals(2, generator.getLevel());
        assertFalse(generator.upgrade());
    }

    @Test
    void diamondGeneratorUsesTheLevelsOfTheCurrentConfig() throws Exception {
        when(arena.getGeneratorsLeveling()).thenReturn(leveling("Default", 3, 3));
        DiamondGenerator generator = new DiamondGenerator(location);
        when(arena.getGeneratorsLeveling()).thenReturn(leveling("Short", 3, 2));

        assertFalse(generator.upgrade(3));
        assertTrue(generator.upgrade(2));
        assertEquals(2, generator.getLevel());
    }

    @Test
    void generatorsWithoutAConfigDoNotUpgrade() throws Exception {
        when(arena.getGeneratorsLeveling()).thenReturn(leveling("Default", 3, 3));
        EmeraldGenerator generator = new EmeraldGenerator(location);
        when(arena.getGeneratorsLeveling()).thenReturn(null);

        assertEquals(0, generator.getMaxLevel());
        assertFalse(generator.upgrade());
    }
}
