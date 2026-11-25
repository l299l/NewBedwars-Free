package com.l299l.newbedwars.config;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdaterCustomEntriesTest {
    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void customTablistLayoutSurvivesReload(@TempDir Path dir) throws Exception {
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        when(plugin.getDataFolder()).thenReturn(dir.toFile());
        when(plugin.getDescription()).thenReturn(new org.bukkit.plugin.PluginDescriptionFile("NewBedwars", "test", "x.Main"));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getResource(anyString())).thenAnswer(i -> getClass().getClassLoader().getResourceAsStream(i.getArgument(0, String.class)));
        File file = new File(dir.toFile(), "tablists.yml");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("tablists.yml")) {
            Files.write(file.toPath(), in.readAllBytes());
        }
        Files.writeString(file.toPath(), Files.readString(file.toPath(), StandardCharsets.UTF_8)
                + "  Mine:\n    Playing:\n      Header:\n        - \"hello {player}\"\n", StandardCharsets.UTF_8);

        Updater updater = new Updater();
        updater.reloadTablists();
        updater.reloadTablists();

        FileConfiguration conf = YamlConfiguration.loadConfiguration(file);
        assertNotNull(conf.getConfigurationSection("Layouts.Mine"));
        assertEquals("hello {player}", conf.getStringList("Layouts.Mine.Playing.Header").get(0));
        assertTrue(conf.contains("Layouts.Default.Playing"));
    }
}
