package com.l299l.newbedwars.arena;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArenaRegistryTest {

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
    }

    private IArena arena(String name) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn(name);
        return arena;
    }

    @Test
    void lookupByNameAndWorld() {
        IArena castle = arena("Castle");
        World world = mock(World.class);
        ArenaRegistry.register("Castle", castle);
        ArenaRegistry.bindWorld(world, castle);

        assertSame(castle, ArenaRegistry.byName("Castle"));
        assertNull(ArenaRegistry.byName("castle"));
        assertSame(castle, ArenaRegistry.find("castle"));
        assertNull(ArenaRegistry.find(null));
        assertSame(castle, ArenaRegistry.byWorld(world));
        assertTrue(ArenaRegistry.contains("Castle"));
    }

    @Test
    void unregisterRemovesNameAndWorld() {
        IArena castle = arena("Castle");
        World world = mock(World.class);
        ArenaRegistry.register("Castle", castle);
        ArenaRegistry.bindWorld(world, castle);
        ArenaRegistry.unregister("Castle", world);
        assertNull(ArenaRegistry.byName("Castle"));
        assertNull(ArenaRegistry.byWorld(world));
        ArenaRegistry.unregister("Missing", null);
    }

    @Test
    void viewsAreLiveAndReadOnly() {
        var all = ArenaRegistry.all();
        ArenaRegistry.register("Castle", arena("Castle"));
        assertEquals(1, all.size());
        assertTrue(ArenaRegistry.names().contains("Castle"));
        assertThrows(UnsupportedOperationException.class, () -> all.clear());
    }

    @SuppressWarnings("deprecation")
    @Test
    void deprecatedMapsAreTheSameMaps() {
        ArenaRegistry.register("Castle", arena("Castle"));
        assertTrue(Arena.arenaByName.containsKey("Castle"));
    }
}
