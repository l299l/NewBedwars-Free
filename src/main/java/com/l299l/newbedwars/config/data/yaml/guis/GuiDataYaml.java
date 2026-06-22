package com.l299l.newbedwars.config.data.yaml.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.GuiSave;
import com.l299l.newbedwars.gui.configuration.game.guis.ShopGUI;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiCategory;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiUpgrade;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class GuiDataYaml {
    GuiSave guiSave;

    public GuiDataYaml(GuiSave guiSave) {
        this.guiSave = guiSave;
    }

    public GuiDataYaml(File gui) {
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            HashMap<String, Object> guiSaveObject = yaml.load(Files.newInputStream(gui.toPath()));
            String guiClass = guiSaveObject.get("GuiClass").toString();
            String guiId = guiSaveObject.get("GuiId").toString();
            String name = guiSaveObject.get("GuiName").toString();
            Integer size = (Integer) guiSaveObject.get("Size");
            Boolean closeOnTransaction = (Boolean) guiSaveObject.get("CloseOnTransaction");
            Class<?> guiClassObj = null;
            if (guiClass.equalsIgnoreCase("ShopGui")) {
                guiClassObj = ShopGUI.class;
            }
            GuiSave guiSave = new GuiSave(guiClassObj, guiId, name, size, closeOnTransaction);
            LinkedHashMap<Integer, String> items = (LinkedHashMap<Integer, String>)guiSaveObject.get("Slots");
            List<Map<String, Object>> categories = (List<Map<String, Object>>) guiSaveObject.get("Categories");
            HashMap<String, GuiCategory> guiCategories = new HashMap<>();
            for (Map<String, Object> category: categories) {
                String catId = category.get("ID").toString();
                String catName = (category.get("Name") != null)
                        ? category.get("Name").toString()
                        : catId.substring(0, 1).toUpperCase() + catId.substring(1).toLowerCase();
                CustomItem catIcon = NewBedwars.plugin.getCustomItemManager().getCustomItem(category.get("Icon").toString());
                List<String> catItemsNames = (List<String>) category.get("Items");
                List<Object> catItems = new ArrayList<>();
                for (String catItemObj: catItemsNames) {
                    String[] catItemData = catItemObj.split(":");
                    if (catItemData.length == 1) {
                        CustomItem catItem = NewBedwars.plugin.getCustomItemManager().getCustomItem(catItemObj);
                        catItems.add(catItem);
                        continue;
                    }
                    String catItemType = catItemData[0];
                    String catItemName = catItemData[1];
                    if (catItemType.equalsIgnoreCase("upgrade") || catItemType.equalsIgnoreCase("upg")) {
                        GuiUpgrade guiUpgrade = getGuiUpgrade(catItemName);
                        catItems.add(guiUpgrade);
                        continue;
                    }
                    CustomItem catItem = NewBedwars.plugin.getCustomItemManager().getCustomItem(catItemName);
                    catItems.add(catItem);
                }
                String catDescription = category.containsKey("Description") ? category.get("Description").toString() : "";
                GuiCategory guiCategory = new GuiCategory(catId, catName, catIcon, catItems, catDescription);
                guiCategories.put(catId, guiCategory);
            }
            guiSave.setGuiData("Categories", guiCategories);
            String[] fill = guiSaveObject.get("Fill").toString().split(":");
            String fillType = fill[0];
            CustomItem fillItem = null;
            if (fillType.equalsIgnoreCase("customItem") || fillType.equalsIgnoreCase("ci")) {
                String itemName = fill[1];
                fillItem = NewBedwars.plugin.getCustomItemManager().getCustomItem(itemName);
            }
            String[] empty = guiSaveObject.get("Empty").toString().split(":");
            String emptyType = empty[0];
            CustomItem emptyItem = null;
            if (emptyType.equalsIgnoreCase("customItem") || emptyType.equalsIgnoreCase("ci")) {
                String itemName = empty[1];
                emptyItem = NewBedwars.plugin.getCustomItemManager().getCustomItem(itemName);
            }
            Integer headerSize = (Integer) guiSaveObject.get("HeaderSize");
            Integer footerSize = (Integer) guiSaveObject.get("FooterSize");
            guiSave.setGuiData("HeaderSize", headerSize);
            guiSave.setGuiData("FooterSize", footerSize);
            for (int key = 0; key < items.size(); key++) {
                String item = items.get(key+1);
                if (item.equalsIgnoreCase("fill")) {
                    guiSave.setItem(key, fillItem);
                }else if (item.equalsIgnoreCase("empty")) {
                    guiSave.setItem(key, emptyItem);
                }else {
                    String[] itemData = item.split(":");
                    String type = itemData[0];
                    if (type.equalsIgnoreCase("customItem") || type.equalsIgnoreCase("ci")) {
                        String itemName = itemData[1];
                        CustomItem customItem = NewBedwars.plugin.getCustomItemManager().getCustomItem(itemName);
                        guiSave.setItem(key, customItem);
                    }else if (type.equalsIgnoreCase("category") || type.equalsIgnoreCase("cat")) {
                        String catName = itemData[1];
                        GuiCategory guiCategory = guiCategories.get(catName);
                        if (guiCategory == null) {
                            throw new RuntimeException("Category " + catName + " not found!");
                        }
                        guiSave.setItem(key, guiCategory);
                    }else if(type.equalsIgnoreCase("upgrade") || type.equalsIgnoreCase("upg")) {
                        String upgradeName = itemData[1];
                        GuiUpgrade guiUpgrade = getGuiUpgrade(upgradeName);
                        guiSave.setItem(key, guiUpgrade);
                    }
                }
            }
            this.guiSave = guiSave;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GuiSave getGuiSave() {
        return guiSave;
    }

    private GuiUpgrade getGuiUpgrade(String upgradeName) {
        Upgrade upgrade = Upgrade.valueOf(upgradeName.toUpperCase());
        CustomItem icon = NewBedwars.plugin.getCustomItemManager().getCustomItem(upgradeName);
        if (icon == null) {
            throw new RuntimeException("Icon for upgrade " + upgradeName + " not found!");
        }
        return new GuiUpgrade(upgradeName, icon, upgrade);
    }
}
