package com.l299l.newbedwars.config.properties;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.config.ModesFile;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertiesGamePhasesTest {
    private static final File BUNDLED = new File("src/main/resources/config.yml");
    private static final File BUNDLED_MODES = new File("src/main/resources/modes.yml");

    private PropertiesSnapshot saved;
    private Logger logger;
    private YamlConfiguration config;
    private Map<String, Object> own = new HashMap<>();
    private Map<String, Object> bundledRoot;

    @BeforeEach
    void setUp() throws Exception {
        saved = PropertiesSnapshot.take();
        Properties.GamePhases = new HashMap<>();
        logger = mock(Logger.class);
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getLogger()).thenReturn(logger);
        config = new YamlConfiguration();
        config.setDefaults(YamlConfiguration.loadConfiguration(BUNDLED));
        when(plugin.getConfig()).thenReturn(config);
        NewBedwars.plugin = plugin;
        bundledRoot = parse(Files.readString(BUNDLED_MODES.toPath()));
        ModesFile.use(Map.of(), bundledRoot);
    }

    @AfterEach
    void tearDown() {
        saved.restore();
        NewBedwars.plugin = null;
        ModesFile.use(null, null);
    }

    private static Map<String, Object> parse(String yaml) {
        return new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
    }

    private void userConfig(String yaml) throws Exception {
        own = parse(yaml);
        ModesFile.use(own, bundledRoot);
    }

    private void load() {
        new Properties();
    }

    @Test
    void userEntriesLoadAndBuiltInsFillTheMissingIds() throws Exception {
        userConfig("""
                GamePhases:
                  - ID: Custom
                    Phases:
                      - Name: First
                        Duration: 30
                        Actions: ["a"]
                      - Name: Last
                """);
        load();
        assertEquals(Set.of("Custom", "Solo", "Doubles", "Squads", "Rush", "Duels", "Default"), Properties.GamePhases.keySet());
        assertEquals("Custom", Properties.GamePhases.get("Custom").getID());
    }

    @Test
    void userEntryWithABuiltInIdWins() throws Exception {
        userConfig("""
                GamePhases:
                  - ID: Solo
                    Phases:
                      - Name: Only
                """);
        load();
        GamePhases solo = Properties.GamePhases.get("Solo");
        assertEquals("Only", solo.getCurrentPhase().getName());
        assertTrue(solo.isLastPhase());
    }

    @Test
    void nonListGamePhasesLoadsOnlyBuiltInsAndWarns() throws Exception {
        userConfig("GamePhases: nope\n");
        load();
        assertEquals(Set.of("Solo", "Doubles", "Squads", "Rush", "Duels", "Default"), Properties.GamePhases.keySet());
        verify(logger, atLeastOnce()).warning(org.mockito.ArgumentMatchers.contains("GamePhases is not a list"));
    }

    @Test
    void bundledConfigLoadsWithoutWarnings() {
        load();
        assertEquals(Set.of("Solo", "Doubles", "Squads", "Rush", "Duels", "Default"), Properties.GamePhases.keySet());
        verify(logger, never()).warning(anyString());
    }

    @Test
    void entryWithoutPhasesIsSkippedWithAWarning() throws Exception {
        userConfig("""
                GamePhases:
                  - ID: Broken
                """);
        load();
        assertFalse(Properties.GamePhases.containsKey("Broken"));
        verify(logger, atLeastOnce()).warning(org.mockito.ArgumentMatchers.contains("without ID or Phases"));
    }
}
