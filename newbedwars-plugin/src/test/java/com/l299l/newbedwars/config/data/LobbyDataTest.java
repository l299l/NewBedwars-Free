package com.l299l.newbedwars.config.data;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LobbyDataTest {

    @TempDir
    Path tempDir;

    private NewBedwars savedPlugin;

    @BeforeEach
    void setUp() {
        savedPlugin = NewBedwars.plugin;
        NewBedwars mockPlugin = mock(NewBedwars.class);
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
        NewBedwars.plugin = mockPlugin;
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = savedPlugin;
    }

    @Test
    void saveAndLoad_roundTrip() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("test_world");
        Location original = new Location(world, 1.5, 64.0, -3.5, 90.0f, 45.0f);

        LobbyData.save(original);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("test_world")).thenReturn(world);
            Location loaded = LobbyData.load();
            assertNotNull(loaded);
            assertEquals(world, loaded.getWorld());
            assertEquals(1.5, loaded.getX(), 1e-6);
            assertEquals(64.0, loaded.getY(), 1e-6);
            assertEquals(-3.5, loaded.getZ(), 1e-6);
            assertEquals(90.0f, loaded.getYaw(), 1e-3f);
            assertEquals(45.0f, loaded.getPitch(), 1e-3f);
        }
    }

    @Test
    void save_nullDoesNotCreateFile() {
        LobbyData.save(null);
        assertFalse(new File(tempDir.toFile(), "data/lobby.yml").exists());
    }

    @Test
    void load_returnsNullWhenFileAbsent() {
        assertNull(LobbyData.load());
    }

    @Test
    void load_returnsNullWhenWorldNotFound() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("gone_world");
        LobbyData.save(new Location(world, 0, 0, 0));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("gone_world")).thenReturn(null);
            assertNull(LobbyData.load());
        }
    }
}
