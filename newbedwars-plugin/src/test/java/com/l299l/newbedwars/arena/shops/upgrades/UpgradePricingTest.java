package com.l299l.newbedwars.arena.shops.upgrades;

import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.UpgradeHandler;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.config.data.yaml.guis.GuiDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UpgradePricingTest {
    @TempDir
    Path dir;
    private final List<String> warnings = new ArrayList<>();
    private Logger log;
    private Upgrade addon;

    @BeforeEach
    void setUp() {
        log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    @AfterEach
    void tearDown() {
        if (addon != null) Upgrade.unregister(addon);
    }

    private UpgradePricing pricing() {
        return new UpgradePricing(dir, Runnable::run, log, 3);
    }

    private Path file() {
        return dir.resolve("data/upgrades.yml");
    }

    private void write(String yaml) throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), yaml, StandardCharsets.UTF_8);
    }

    private static UpgradeTier t(int price, PriceType currency) {
        return new UpgradeTier(price, currency);
    }

    @Test
    void firstStartWritesTheBundledPrices() throws IOException {
        UpgradePricing p = pricing();
        p.load(null);
        assertEquals(5, p.tiers(Upgrade.PROTECTION).size());
        assertEquals(t(16, PriceType.DIAMOND), p.tiers(Upgrade.PROTECTION).get(4));
        assertTrue(Files.isRegularFile(file()));
        UpgradePricing again = pricing();
        again.load(null);
        assertEquals(p.tiers(Upgrade.FORGE), again.tiers(Upgrade.FORGE));
    }

    @Test
    void oldConfigPricesAreMovedOnce() throws IOException {
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("PriceType", "emerald");
        legacy.put("PROTECTION", List.of(1, 2));
        legacy.put("haste", List.of(7));
        UpgradePricing p = pricing();
        p.load(legacy);
        assertEquals(List.of(t(1, PriceType.EMERALD), t(2, PriceType.EMERALD)), p.tiers(Upgrade.PROTECTION));
        assertEquals(List.of(t(7, PriceType.EMERALD)), p.tiers(Upgrade.HASTE));
        assertEquals(4, p.tiers(Upgrade.SHARPNESS).size());
        assertEquals(PriceType.EMERALD, p.getDefaultCurrency());
        assertTrue(Files.readString(file()).contains("currency: EMERALD"));

        Map<String, Object> other = Map.of("PROTECTION", List.of(99));
        UpgradePricing later = pricing();
        later.load(other);
        assertEquals(2, later.tiers(Upgrade.PROTECTION).size());
    }

    @Test
    void readsShortAndLongFormsAndKeepsBrokenEntries() throws IOException {
        write("""
                DefaultCurrency: GOLD
                Upgrades:
                  haste: [1, 2]
                  SHARPNESS:
                    tiers:
                      - {price: 3, currency: iron}
                  FORGE:
                    - {price: x}
                  HEALPOOL: []
                """);
        UpgradePricing p = pricing();
        p.load(null);
        assertEquals(List.of(t(1, PriceType.GOLD), t(2, PriceType.GOLD)), p.tiers(Upgrade.HASTE));
        assertEquals(List.of(t(3, PriceType.IRON)), p.tiers(Upgrade.SHARPNESS));
        assertTrue(p.isBroken("forge"));
        assertTrue(p.tiers(Upgrade.FORGE).isEmpty());
        assertNull(p.next(Upgrade.FORGE, 0, null));
        assertNull(p.next(Upgrade.HEALPOOL, 0, null));
        assertTrue(p.tiers(Upgrade.PROTECTION).isEmpty());

        p.set("haste", List.of(t(5, PriceType.EMERALD)));
        String saved = Files.readString(file());
        assertTrue(saved.contains("FORGE: [{price: x}]"), saved);
        UpgradePricing again = pricing();
        again.load(null);
        assertEquals(List.of(t(5, PriceType.EMERALD)), again.tiers(Upgrade.HASTE));
        assertEquals(List.of(t(3, PriceType.IRON)), again.tiers(Upgrade.SHARPNESS));
        assertTrue(again.isBroken("FORGE"));
    }

    @Test
    void levelsAboveTheCapAreIgnoredWithAWarning() throws IOException {
        write("""
                Upgrades:
                  HEALPOOL: [1, 2, 3]
                """);
        UpgradePricing p = pricing();
        p.load(null);
        assertEquals(1, p.maxLevel(Upgrade.HEALPOOL, null));
        assertEquals(t(1, PriceType.DIAMOND), p.next(Upgrade.HEALPOOL, 0, null));
        assertNull(p.next(Upgrade.HEALPOOL, 1, null));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("HEALPOOL")));
    }

    @Test
    void guiOverrideWinsOverTheGlobalPrices() {
        UpgradePricing p = pricing();
        p.load(null);
        GuiDefinition gui = new GuiDefinition("solo", "ShopGUI", "T", 9, "empty", "empty", 0, 0,
                List.of(), List.of(), Map.of(UpgradePricing.GUI_KEY, Map.of(
                "forge", List.of(Map.of("price", 1, "currency", "IRON")),
                "HASTE", "broken")));
        assertEquals(List.of(t(1, PriceType.IRON)), p.tiers(Upgrade.FORGE, gui));
        assertEquals(1, p.maxLevel(Upgrade.FORGE, gui));
        assertEquals(p.tiers(Upgrade.HASTE), p.tiers(Upgrade.HASTE, gui));
        assertEquals(p.tiers(Upgrade.FORGE), p.tiers(Upgrade.FORGE, null));
        p.tiers(Upgrade.HASTE, gui);
        assertEquals(1, warnings.stream().filter(w -> w.contains("HASTE") && w.contains("solo")).count());
    }

    @Test
    void addonDefaultsAreUsedAndWrittenOnce() throws IOException {
        addon = Upgrade.register("thornsTest", 3, new UpgradeHandler() {}, List.of(4, 6));
        UpgradePricing p = pricing();
        p.load(null);
        assertEquals(List.of(t(4, PriceType.DIAMOND), t(6, PriceType.DIAMOND)), p.tiers(addon));
        assertFalse(p.isConfigured("THORNSTEST"));
        p.addMissing(addon);
        assertTrue(Files.readString(file()).contains("THORNSTEST:"));
        p.set("THORNSTEST", List.of(t(1, PriceType.GOLD)));
        p.addMissing(addon);
        assertEquals(List.of(t(1, PriceType.GOLD)), p.tiers(addon));
    }

    @Test
    void tierRejectsBadValues() {
        assertThrows(IllegalArgumentException.class, () -> new UpgradeTier(-1, PriceType.IRON));
        assertThrows(IllegalArgumentException.class, () -> new UpgradeTier(1, PriceType.NONE));
        assertThrows(IllegalArgumentException.class, () -> UpgradeTier.parseCurrency("WOOD", PriceType.IRON));
        assertEquals(PriceType.IRON, UpgradeTier.parseCurrency(null, PriceType.IRON));
        assertThrows(IllegalArgumentException.class, () -> UpgradePricing.parseTiers(List.of(-3), PriceType.IRON));
        assertThrows(IllegalArgumentException.class, () -> UpgradePricing.parseTiers("x", PriceType.IRON));
    }

    @Test
    void anUnreadableFileIsNeverSavedOver() throws IOException {
        write("Upgrades: [broken");
        UpgradePricing p = pricing();
        p.load(null);
        assertTrue(p.isReadOnly());
        p.save();
        assertEquals("Upgrades: [broken", Files.readString(file(), StandardCharsets.UTF_8));
    }
}
