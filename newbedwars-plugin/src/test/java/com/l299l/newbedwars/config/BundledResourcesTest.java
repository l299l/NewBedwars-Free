package com.l299l.newbedwars.config;

import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.shops.upgrades.UpgradePricing;
import com.l299l.newbedwars.config.data.yaml.guis.GuiCodec;
import com.l299l.newbedwars.config.data.yaml.guis.GuiDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledResourcesTest {
    private static final String RES = "src/main/resources/";
    private static final List<String> MODES = List.of("Solo", "Doubles", "Squads", "Rush", "Duels");

    @ParameterizedTest
    @ValueSource(strings = {"Solo", "Doubles", "Squads", "Rush", "Duels"})
    void generatorPresetParsesAndKeepsItsId(String mode) {
        File file = new File(RES + "generators/" + mode + ".yml");
        assertTrue(file.isFile());
        GeneratorLeveling leveling = new GeneratorLeveling(YamlConfiguration.loadConfiguration(file));
        assertEquals(mode, leveling.getId());
        assertNotNull(leveling.getDefaultGeneratorSettings());
        assertNotNull(leveling.getDiamondGeneratorSettings());
        assertNotNull(leveling.getEmeraldGeneratorSettings());
    }

    @Test
    void everyGeneratorFileIsCovered() {
        String[] files = new File(RES + "generators").list();
        assertNotNull(files);
        Set<String> names = new java.util.TreeSet<>();
        for (String f : files) names.add(f.replace(".yml", ""));
        names.remove("defaultGenerators");
        assertEquals(new java.util.TreeSet<>(MODES), names);
    }

    @Test
    void configDefaultsToSquadsAndHasNoModesOrPhases() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(RES + "config.yml"));
        assertFalse(config.contains("GamePhases"));
        assertFalse(config.contains("Modes"));
        assertEquals("Squads", config.getString("DefaultGeneratorConfig"));
        assertEquals("Squads", config.getString("DefaultGamePhase"));
    }

    @Test
    void modesFileListsEveryModeAndPhase() throws Exception {
        Map<String, Object> root = modesRoot();
        List<String> ids = new ArrayList<>();
        for (Object entry : (List<?>) root.get("GamePhases")) {
            ids.add(String.valueOf(((Map<?, ?>) entry).get("ID")));
        }
        assertEquals(List.of("Solo", "Doubles", "Squads", "Rush", "Duels", "Default"), ids);
        assertEquals(MODES, new ArrayList<>(((Map<?, ?>) root.get("Modes")).keySet()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> modesRoot() throws Exception {
        return (Map<String, Object>) new Yaml(new SafeConstructor(new LoaderOptions())).load(Files.readString(Path.of(RES + "modes.yml")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Solo", "Doubles", "Squads", "Rush", "Duels"})
    void upgradeGuiParsesWithValidPrices(String mode) throws Exception {
        String id = "upgrade" + mode;
        GuiDefinition gui = GuiCodec.parse(Files.readString(Path.of(RES + "guis/" + id + ".yml")));
        assertEquals(id, gui.id());
        List<String> warnings = new ArrayList<>();
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            public void publish(LogRecord r) {
                warnings.add(r.getMessage());
            }

            public void flush() {
            }

            public void close() {
            }
        });
        UpgradePricing pricing = new UpgradePricing(Files.createTempDirectory("up"), Runnable::run, log, 3);
        assertTrue(gui.extra().get(UpgradePricing.GUI_KEY) instanceof java.util.Map<?, ?>);
        assertTrue(!pricing.overrides(gui).isEmpty());
        assertEquals(((java.util.Map<?, ?>) gui.extra().get(UpgradePricing.GUI_KEY)).size(), pricing.overrides(gui).size());
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void everyUpgradeGuiFileIsCovered() {
        String[] files = new File(RES + "guis").list();
        assertNotNull(files);
        Set<String> names = new java.util.TreeSet<>();
        for (String f : files) names.add(f.replace(".yml", ""));
        names.remove("exampleShop");
        names.remove("exampleUpgrade");
        Set<String> expected = new java.util.TreeSet<>();
        for (String m : MODES) expected.add("upgrade" + m);
        assertEquals(expected, names);
    }

    @Test
    void bundledModesAreTheFiveBuiltInsAndReferenceBundledParts() throws Exception {
        Map<String, Object> root = modesRoot();
        Map<?, ?> modes = (Map<?, ?>) root.get("Modes");
        assertNotNull(modes);
        assertEquals(MODES, new ArrayList<>(modes.keySet().stream().map(String::valueOf).toList()));
        Set<String> phaseIds = new java.util.HashSet<>();
        for (Object entry : (List<?>) root.get("GamePhases")) phaseIds.add(String.valueOf(((Map<?, ?>) entry).get("ID")));
        for (String id : MODES) {
            Map<?, ?> mode = (Map<?, ?>) modes.get(id);
            assertNotNull(mode, id);
            String generators = (String) mode.get("Generators");
            assertNotNull(generators, id);
            File file = new File(RES + "generators/" + generators + ".yml");
            assertTrue(file.isFile(), generators);
            assertEquals(generators, new GeneratorLeveling(YamlConfiguration.loadConfiguration(file)).getId());
            assertTrue(phaseIds.contains((String) mode.get("GamePhases")), id);
            String upgrade = (String) mode.get("UpgradeGui");
            assertNotNull(upgrade, id);
            assertEquals(upgrade, GuiCodec.parse(Files.readString(Path.of(RES + "guis/" + upgrade + ".yml"))).id());
            assertTrue((Integer) mode.get("MaxInTeam") > 0, id);
            assertTrue((Integer) mode.get("Teams") > 0, id);
        }
    }
}
