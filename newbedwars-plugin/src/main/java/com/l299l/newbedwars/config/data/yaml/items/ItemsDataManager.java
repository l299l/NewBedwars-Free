package com.l299l.newbedwars.config.data.yaml.items;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.config.data.yaml.SafeYaml;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemsDataManager {
    private final Path file;
    private final Path backups;
    private final CustomItemManager customItemManager;
    private final Consumer<Runnable> io;
    private final Logger log;
    private final int keep;
    private final Map<String, Object> broken = new LinkedHashMap<>();
    private final List<String> order = new ArrayList<>();

    public ItemsDataManager() {
        this(NewBedwars.plugin.getDataFolder().toPath(), NewBedwars.plugin.getCustomItemManager(), IOExecutor::submit,
                NewBedwars.plugin.getLogger(), Math.max(1, Properties.BackupsKeep));
    }

    public ItemsDataManager(Path dataFolder, CustomItemManager customItemManager, Consumer<Runnable> io, Logger log, int keep) {
        this.file = dataFolder.resolve("data/items.yml");
        this.backups = dataFolder.resolve("data/backups/shop/items");
        this.customItemManager = customItemManager;
        this.io = io;
        this.log = log;
        this.keep = keep;
    }

    public void load() {
        customItemManager.clear();
        broken.clear();
        order.clear();
        if (!Files.isRegularFile(file)) {
            customItemManager.createDefaults();
            save();
            return;
        }
        Object root;
        try {
            root = SafeYaml.create().load(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Could not read " + file + "; no shop items are loaded and the file is not changed", e);
            return;
        }
        if (!(root instanceof Map<?, ?> items)) {
            log.severe(file + " is not a YAML map; no shop items are loaded");
            return;
        }
        for (Map.Entry<?, ?> e : items.entrySet()) {
            String name = String.valueOf(e.getKey());
            order.add(name);
            try {
                if (!(e.getValue() instanceof Map<?, ?> item)) throw new IllegalArgumentException("not a map");
                loadItem(name, item);
            } catch (Exception ex) {
                broken.put(name, e.getValue());
                log.warning("Shop item " + name + " in items.yml was not loaded: " + ex.getMessage()
                        + " (it is kept in the file unchanged)");
            }
        }
    }

    private void loadItem(String name, Map<?, ?> item) {
        Material material = Material.matchMaterial(text(item, "material", ""));
        if (material == null) throw new IllegalArgumentException("unknown material " + item.get("material"));
        HashMap<Enchantment, Integer> enchantments = new HashMap<>();
        if (item.get("enchantments") instanceof Map<?, ?> ench) {
            for (Map.Entry<?, ?> en : ench.entrySet()) {
                NamespacedKey key = NamespacedKey.fromString(String.valueOf(en.getKey()).toLowerCase());
                Enchantment enchantment = key == null ? null : Enchantment.getByKey(key);
                if (enchantment == null) throw new IllegalArgumentException("unknown enchantment " + en.getKey());
                enchantments.put(enchantment, number(en.getValue(), 1));
            }
        }
        List<ItemFlag> flags = new ArrayList<>();
        if (item.get("itemFlags") instanceof List<?> list) {
            for (Object f : list) {
                try {
                    flags.add(ItemFlag.valueOf(String.valueOf(f)));
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("unknown item flag " + f);
                }
            }
        }
        PriceType priceType = PriceType.valueOf(text(item, "priceType", "NONE").toUpperCase());
        OnBuyEvent onBuy = OnBuyEvent.valueOf(text(item, "onBuyEvent", "GIVE_ITEM").toUpperCase());
        customItemManager.createCustomItem(name, text(item, "description", "default"), material,
                number(item.get("price"), 0), priceType, number(item.get("amount"), 1),
                text(item, "iconDesc", "default"), text(item, "permission", ""), text(item, "itemLogic", "NONE"),
                onBuy, Boolean.parseBoolean(String.valueOf(item.get("isPermanent"))), enchantments, flags, true);
        NewBedwars.plugin.getLangMessages().addCustomItemProperty(name, "-name");
    }

    public void remove(String name) {
        order.removeIf(n -> n.equalsIgnoreCase(name));
        broken.keySet().removeIf(n -> n.equalsIgnoreCase(name));
    }

    public boolean isBroken(String name) {
        for (String n : broken.keySet()) if (n.equalsIgnoreCase(name)) return true;
        return false;
    }

    public List<String> getBrokenNames() {
        return new ArrayList<>(broken.keySet());
    }

    public String toYaml() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String name : order) {
            if (broken.containsKey(name)) {
                out.put(name, broken.get(name));
            } else {
                CustomItem item = customItemManager.getCustomItem(name);
                if (item != null) out.put(item.getName(), item.getAsHashMap());
            }
        }
        for (CustomItem item : customItemManager.getCustomItems()) {
            if (!out.containsKey(item.getName())) out.put(item.getName(), item.getAsHashMap());
        }
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options).dump(out);
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

    private static String text(Map<?, ?> map, String key, String fallback) {
        Object o = map.get(key);
        return o == null ? fallback : o.toString();
    }

    private static int number(Object o, int fallback) {
        if (o instanceof Number n) return n.intValue();
        if (o == null) return fallback;
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
