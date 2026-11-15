package com.l299l.newbedwars.arena.shops.upgrades;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.config.data.yaml.SafeYaml;
import com.l299l.newbedwars.config.data.yaml.guis.GuiDefinition;
import com.l299l.newbedwars.config.properties.Properties;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpgradePricing {
    public static final String GUI_KEY = "UpgradePrices";
    private static final String HEADER = """
            # Upgrade and trap prices. Each entry is the list of levels a team can buy, in order:
            # the first entry is the price of level 1, the second of level 2 and so on.
            # currency: IRON, GOLD, DIAMOND or EMERALD (DefaultCurrency when left out).
            # A shorter list means fewer levels; an empty list means the upgrade can not be bought.
            # An upgrade GUI file in data/guis/ can override prices for arenas that use it with its own
            # "UpgradePrices:" section in the same format.
            """;

    private final Path file;
    private final Path backups;
    private final Consumer<Runnable> io;
    private final Logger log;
    private final int keep;
    private PriceType defaultCurrency = PriceType.DIAMOND;
    private final Map<String, List<UpgradeTier>> tiers = new LinkedHashMap<>();
    private final Map<String, Object> broken = new LinkedHashMap<>();

    public UpgradePricing() {
        this(NewBedwars.plugin.getDataFolder().toPath(), IOExecutor::submit, NewBedwars.plugin.getLogger(),
                Math.max(1, Properties.BackupsKeep));
    }

    public UpgradePricing(Path dataFolder, Consumer<Runnable> io, Logger log, int keep) {
        this.file = dataFolder.resolve("data/upgrades.yml");
        this.backups = dataFolder.resolve("data/backups/shop/upgrades");
        this.io = io;
        this.log = log;
        this.keep = keep;
    }

    public void load(Map<String, Object> legacyConfig) {
        tiers.clear();
        broken.clear();
        defaultCurrency = PriceType.DIAMOND;
        if (!Files.isRegularFile(file)) {
            if (legacyConfig != null && !legacyConfig.isEmpty()) {
                loadLegacy(legacyConfig);
                log.info("Upgrade prices were moved from config.yml (UpgradePrices) to data/upgrades.yml");
            } else {
                loadBundled();
            }
            save();
            return;
        }
        Object root;
        try {
            root = SafeYaml.create().load(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Could not read " + file + "; no upgrade can be bought until it is fixed", e);
            return;
        }
        if (!(root instanceof Map<?, ?> map)) {
            log.severe(file + " is not a YAML map; no upgrade can be bought until it is fixed");
            return;
        }
        read(map);
    }

    private void loadBundled() {
        try (InputStream in = UpgradePricing.class.getResourceAsStream("/upgrades.yml")) {
            if (in == null) return;
            Object root = SafeYaml.create().load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            if (root instanceof Map<?, ?> map) read(map);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read the default upgrade prices", e);
        }
    }

    private void loadLegacy(Map<String, Object> legacy) {
        loadBundled();
        try {
            defaultCurrency = UpgradeTier.parseCurrency(legacy.get("PriceType"), PriceType.DIAMOND);
        } catch (IllegalArgumentException e) {
            defaultCurrency = PriceType.DIAMOND;
        }
        for (Map.Entry<String, Object> e : legacy.entrySet()) {
            if (e.getKey().equalsIgnoreCase("PriceType") || !(e.getValue() instanceof List<?>)) continue;
            try {
                tiers.put(key(e.getKey()), parseTiers(e.getValue(), defaultCurrency));
            } catch (IllegalArgumentException ex) {
                log.warning("Upgrade price " + e.getKey() + " in config.yml was not moved: " + ex.getMessage());
            }
        }
    }

    private void read(Map<?, ?> root) {
        try {
            defaultCurrency = UpgradeTier.parseCurrency(root.get("DefaultCurrency"), PriceType.DIAMOND);
        } catch (IllegalArgumentException e) {
            log.warning("DefaultCurrency in upgrades.yml: " + e.getMessage() + "; DIAMOND is used");
        }
        if (!(root.get("Upgrades") instanceof Map<?, ?> upgrades)) return;
        for (Map.Entry<?, ?> e : upgrades.entrySet()) {
            String name = key(String.valueOf(e.getKey()));
            try {
                List<UpgradeTier> list = parseTiers(e.getValue(), defaultCurrency);
                tiers.put(name, list);
                warnAboveCap(name, list);
            } catch (IllegalArgumentException ex) {
                broken.put(name, e.getValue());
                log.warning("Upgrade " + name + " in upgrades.yml was not loaded: " + ex.getMessage()
                        + " (it can not be bought; the entry is kept in the file unchanged)");
            }
        }
    }

    private void warnAboveCap(String name, List<UpgradeTier> list) {
        Upgrade upgrade = find(name);
        if (upgrade != null && list.size() > upgrade.maxLevel) {
            log.warning("Upgrade " + name + " in upgrades.yml has " + list.size() + " levels but supports at most "
                    + upgrade.maxLevel + "; the extra levels are ignored");
        }
    }

    public List<UpgradeTier> tiers(Upgrade upgrade) {
        List<UpgradeTier> configured = tiers.get(upgrade.name());
        if (configured != null) return configured;
        if (broken.containsKey(upgrade.name())) return List.of();
        List<UpgradeTier> defaults = new ArrayList<>();
        for (int price : upgrade.defaultPrices()) defaults.add(new UpgradeTier(Math.max(0, price), defaultCurrency));
        return defaults;
    }

    public List<UpgradeTier> tiers(Upgrade upgrade, GuiDefinition gui) {
        List<UpgradeTier> override = overrides(gui).get(upgrade.name());
        return override != null ? override : tiers(upgrade);
    }

    public int maxLevel(Upgrade upgrade, GuiDefinition gui) {
        return Math.min(tiers(upgrade, gui).size(), upgrade.maxLevel);
    }

    public UpgradeTier next(Upgrade upgrade, int currentLevel, GuiDefinition gui) {
        if (currentLevel < 0 || currentLevel >= maxLevel(upgrade, gui)) return null;
        return tiers(upgrade, gui).get(currentLevel);
    }

    public Map<String, List<UpgradeTier>> overrides(GuiDefinition gui) {
        if (gui == null || !(gui.extra().get(GUI_KEY) instanceof Map<?, ?> map)) return Map.of();
        Map<String, List<UpgradeTier>> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            try {
                out.put(key(String.valueOf(e.getKey())), parseTiers(e.getValue(), defaultCurrency));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    public PriceType getDefaultCurrency() {
        return defaultCurrency;
    }

    public boolean isBroken(String name) {
        return broken.containsKey(key(name));
    }

    public boolean isConfigured(String name) {
        return tiers.containsKey(key(name));
    }

    public void set(String name, List<UpgradeTier> list) {
        String k = key(name);
        broken.remove(k);
        tiers.put(k, List.copyOf(list));
        save();
    }

    public void addMissing(Upgrade upgrade) {
        if (isConfigured(upgrade.name()) || isBroken(upgrade.name())) return;
        tiers.put(upgrade.name(), List.copyOf(tiers(upgrade)));
        save();
    }

    public String toYaml() {
        StringBuilder sb = new StringBuilder(HEADER);
        sb.append("DefaultCurrency: ").append(defaultCurrency.name()).append("\n\nUpgrades:\n");
        Map<String, Object> all = new LinkedHashMap<>(tiers);
        for (Map.Entry<String, Object> e : broken.entrySet()) all.putIfAbsent(e.getKey(), e.getValue());
        if (all.isEmpty()) return sb.append("  {}\n").toString();
        for (Map.Entry<String, Object> e : all.entrySet()) {
            sb.append("  ").append(e.getKey()).append(':');
            if (e.getValue() instanceof List<?> list && (list.isEmpty() || list.get(0) instanceof UpgradeTier)) {
                if (list.isEmpty()) {
                    sb.append(" []\n");
                    continue;
                }
                sb.append('\n');
                for (Object o : list) {
                    UpgradeTier t = (UpgradeTier) o;
                    sb.append("    - {price: ").append(t.price()).append(", currency: ").append(t.currency().name()).append("}\n");
                }
            } else {
                sb.append(' ').append(flow(e.getValue()));
            }
        }
        return sb.toString();
    }

    public void save() {
        String yaml = toYaml();
        io.accept(() -> {
            try {
                AtomicFileWriter.write(file, yaml, backups, keep, ".yml");
            } catch (IOException e) {
                log.log(Level.SEVERE, "Could not save " + file, e);
            }
        });
    }

    public static List<Map<String, Object>> serialize(List<UpgradeTier> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (UpgradeTier t : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("price", t.price());
            m.put("currency", t.currency().name());
            out.add(m);
        }
        return out;
    }

    public static List<UpgradeTier> parseTiers(Object value, PriceType defaultCurrency) {
        if (value instanceof Map<?, ?> map && map.containsKey("tiers")) value = map.get("tiers");
        if (value == null) return List.of();
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("expected a list of levels");
        List<UpgradeTier> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> tier) {
                out.add(new UpgradeTier(price(tier.get("price")), UpgradeTier.parseCurrency(tier.get("currency"), defaultCurrency)));
            } else {
                out.add(new UpgradeTier(price(o), defaultCurrency));
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static int price(Object o) {
        int value;
        if (o instanceof Number n) {
            value = n.intValue();
        } else {
            try {
                value = Integer.parseInt(String.valueOf(o).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("price " + o + " is not a number");
            }
        }
        if (value < 0) throw new IllegalArgumentException("price " + value + " is negative");
        return value;
    }

    private static Upgrade find(String name) {
        try {
            return Upgrade.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String key(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }

    private static String flow(Object value) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        options.setWidth(Integer.MAX_VALUE);
        return new Yaml(options).dump(value);
    }
}
