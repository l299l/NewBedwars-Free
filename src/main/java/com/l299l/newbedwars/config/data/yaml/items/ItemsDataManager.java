package com.l299l.newbedwars.config.data.yaml.items;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemsDataManager {
    private final String ItemsDataPath = new File(NewBedwars.plugin.getDataFolder(), "data/items.yml").getPath();
    private final CustomItemManager customItemManager;

    public ItemsDataManager() {
        customItemManager = NewBedwars.plugin.getCustomItemManager();
    }

    public void load() {
        File itemsDataFile = new File(ItemsDataPath);
        if (itemsDataFile.exists()) {
            try {
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(options);
                HashMap<String, HashMap<String, Object>> customItems = yaml.load(Files.newInputStream(itemsDataFile.toPath()));
                for (String key: customItems.keySet()) {
                    HashMap<String, Object> item = customItems.get(key);
                    HashMap<String, Integer> enchantments = (HashMap<String, Integer>) item.get("enchantments");
                    HashMap<Enchantment, Integer> enchantments1 = new HashMap<>();
                    for (String enchantment: enchantments.keySet()) {
                        enchantments1.put(Enchantment.getByKey(NamespacedKey.fromString(enchantment)), enchantments.get(enchantment));
                    }
                    List<String> itemFlags = (List<String>) item.get("itemFlags");
                    List<ItemFlag> itemFlags1 = new ArrayList<>();
                    for (String itemFlag: itemFlags) {
                        itemFlags1.add(ItemFlag.valueOf(itemFlag));
                    }
                    customItemManager.createCustomItem(key, item.get("description").toString(),
                            Material.matchMaterial(item.get("material").toString()), (int) item.get("price"), PriceType.valueOf(item.get("priceType").toString()),
                            (int) item.get("amount"), item.get("iconDesc").toString(), item.get("permission").toString(), LogicType.valueOf(item.get("itemLogic").toString()),
                            OnBuyEvent.valueOf(item.get("onBuyEvent").toString()), Boolean.parseBoolean(item.get("isPermanent").toString()),
                            enchantments1, itemFlags1, true);
                    NewBedwars.plugin.getLangMessages().addCustomItemProperty(key, "-name");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            customItemManager.createDefaults();
            save();
        }
    }


    public void save() {
        File itemsDataFile = new File(ItemsDataPath);
        try {
            HashMap<String, HashMap<String, Object>> customItems = customItemManager.getCustomItemsAsHashMap();
            PrintWriter writer = new PrintWriter(itemsDataFile);
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(customItems, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
