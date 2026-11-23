package com.l299l.newbedwars.config.properties;

import com.l299l.newbedwars.NewBedwars;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertiesModesTest {
    private static final File BUNDLED = new File("src/main/resources/config.yml");
    private static final File BUNDLED_MODES = new File("src/main/resources/modes.yml");
    private static final Set<String> BUILT_INS = Set.of("Solo", "Doubles", "Squads", "Rush", "Duels");

    private Map<String, GameMode> saved;
    private Logger logger;
    private YamlConfiguration config;
    private Map<String, Object> own = new HashMap<>();
    private Map<String, Object> bundledRoot;

    @BeforeEach
    void setUp() throws Exception {
        saved = Properties.Modes;
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
        Properties.Modes = saved;
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
    void bundledConfigLoadsBuiltInModesWithoutWarnings() {
        load();
        assertEquals(BUILT_INS, Properties.Modes.keySet());
        GameMode squads = Properties.Modes.get("Squads");
        assertEquals(new GameMode("Squads", "Squads", "Squads", null, "upgradeSquads", 4, 4), squads);
        verify(logger, never()).warning(anyString());
    }

    @Test
    void userModeLoadsAndBuiltInsAreAdded() throws Exception {
        userConfig("""
                Modes:
                  Custom:
                    Generators: Fast
                    GamePhases: Short
                    ShopGui: myShop
                    UpgradeGui: myUpgrade
                    MaxInTeam: 3
                    Teams: 6
                """);
        load();
        Set<String> expected = new java.util.HashSet<>(BUILT_INS);
        expected.add("Custom");
        assertEquals(expected, Properties.Modes.keySet());
        assertEquals(new GameMode("Custom", "Fast", "Short", "myShop", "myUpgrade", 3, 6), Properties.Modes.get("Custom"));
        assertEquals(4, Properties.Modes.get("Squads").maxInTeam());
    }

    @Test
    void userModeWithBuiltInNameWinsAndDoesNotInheritKeys() throws Exception {
        userConfig("""
                Modes:
                  Solo:
                    Generators: Mine
                """);
        load();
        assertEquals(new GameMode("Solo", "Mine", null, null, null, 0, 0), Properties.Modes.get("Solo"));
        assertEquals(BUILT_INS, Properties.Modes.keySet());
    }

    @Test
    void nonSectionModesWarnsAndLoadsBuiltIns() throws Exception {
        userConfig("Modes: nope\n");
        load();
        assertEquals(BUILT_INS, Properties.Modes.keySet());
        verify(logger, atLeastOnce()).warning(contains("Modes is not a section"));
    }

    @Test
    void nonSectionModeEntryIsSkippedWithAWarning() throws Exception {
        userConfig("""
                Modes:
                  Broken: 5
                """);
        load();
        assertNull(Properties.Modes.get("Broken"));
        verify(logger, atLeastOnce()).warning(contains("mode 'Broken' is not a section"));
    }

    @Test
    void invalidNumbersBecomeZero() throws Exception {
        userConfig("""
                Modes:
                  A:
                    MaxInTeam: 0
                    Teams: -2
                  B:
                    MaxInTeam: "four"
                    Teams: 2.5
                  C:
                    MaxInTeam: 5
                    Teams: 7
                """);
        load();
        assertEquals(0, Properties.Modes.get("A").maxInTeam());
        assertEquals(0, Properties.Modes.get("A").teams());
        assertEquals(0, Properties.Modes.get("B").maxInTeam());
        assertEquals(0, Properties.Modes.get("B").teams());
        assertEquals(5, Properties.Modes.get("C").maxInTeam());
        assertEquals(7, Properties.Modes.get("C").teams());
    }

    @Test
    void blankTextValuesAreNull() throws Exception {
        userConfig("""
                Modes:
                  A:
                    Generators: "   "
                    GamePhases: "  Padded  "
                """);
        load();
        assertNull(Properties.Modes.get("A").generators());
        assertEquals("Padded", Properties.Modes.get("A").gamePhases());
    }
}
