package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.GuiSave;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiCategory;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiUpgrade;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI extends BasicGUI {
    private Inventory inv;
    private final List<GuiCategory> categories;
    private final GuiSave guiSave;
    private final GuiCategory guiCategory;
    private final List<Object> items;
    public ShopGUI(GuiManager guiManager, Player player, GuiSave guiSave, GuiCategory category) {
        super(guiManager, player);
        inv = null;
        categories = new ArrayList<>();
        items = new ArrayList<>();
        this.guiSave = guiSave;
        this.guiCategory = category;
        init(guiSave);
    }

    private void init(GuiSave guiSave) {
        inv = Bukkit.createInventory(this, guiSave.getSize(), guiSave.getName());
        for (int i = 0; i < guiSave.getSize(); i++) {
            items.add(null);
        }
        int headerSize = (int) guiSave.getGuiData("HeaderSize");
        int footerSize = (int) guiSave.getGuiData("FooterSize");
        int contentSize = guiSave.getSize() - headerSize - footerSize;
        for (int i = 0; i < headerSize; i++) {
            Object item = guiSave.getItem(i);
            if (item instanceof CustomItem customItem) {
                inv.setItem(i, customItem.getIcon(getPlayer()));
                items.set(i, customItem);
            } else if (item instanceof GuiCategory category) {
                inv.setItem(i, category.getIcon(getPlayer()));
                items.set(i, category);
                categories.add(category);
            }
        }
        if (guiCategory == null || guiCategory.id().isEmpty()|| guiCategory.id().equalsIgnoreCase("Home")) {
            for (int i = headerSize; i < guiSave.getItems().size(); i++) {
                Object item = guiSave.getItem(i);
                if (item instanceof CustomItem customItem) {
                    inv.setItem(i, customItem.getIcon(getPlayer()));
                    items.set(i, customItem);
                } else if (item instanceof GuiCategory category) {
                    inv.setItem(i, category.getIcon(getPlayer()));
                    items.set(i, category);
                    categories.add(category);
                } else if (item instanceof GuiUpgrade guiUpgrade) {
                    inv.setItem(i, guiUpgrade.getIcon(getPlayer()));
                    items.set(i, guiUpgrade);
                }
            }
        }else {
            int itemsSize = guiCategory.items().size();
            int itemsInLine = 5;
            int marginSize = 2;
            int lines = (contentSize / 9);
            if (itemsInLine *  lines< itemsSize) {
                itemsInLine = 7;
                marginSize = 1;
                if (itemsInLine * lines < itemsSize) {
                    itemsInLine = 9;
                    marginSize = 0;
                }
            }
            int i = headerSize;
            int j = 0;
            boolean end = false;
            for (int l = 0; l < lines; l++) {
                i += marginSize;
                for (int k = 0; k < itemsInLine; k++) {
                    Object itemO = guiCategory.items().get(j);
                    if (itemO instanceof GuiUpgrade guiUpgrade) {
                        inv.setItem(i, guiUpgrade.getIcon(getPlayer()));
                        items.set(i, guiUpgrade);
                    } else if (itemO instanceof CustomItem customItem) {
                        inv.setItem(i, customItem.getIcon(getPlayer()));
                        items.set(i, customItem);
                    }
                    i++;
                    j++;
                    if (j >= itemsSize) {
                        end = true;
                        break;
                    }
                }
                i += marginSize;
                if (end) {
                    break;
                }
            }
            i = headerSize + contentSize;
            for (; i < guiSave.getItems().size(); i++) {
                Object item = guiSave.getItem(i);
                if (item instanceof CustomItem customItem) {
                    inv.setItem(i, customItem.getIcon(getPlayer()));
                    items.set(i, customItem);
                } else if (item instanceof GuiCategory category) {
                    inv.setItem(i, category.getIcon(getPlayer()));
                    items.set(i, category);
                    categories.add(category);
                } else if (item instanceof GuiUpgrade guiUpgrade) {
                    inv.setItem(i, guiUpgrade.getIcon(getPlayer()));
                    items.set(i, guiUpgrade);
                }
            }
        }

    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public List<GuiCategory> getCategories() {
        return categories;
    }
    public GuiSave getGuiSave() {return guiSave;}

    public GuiCategory getActualGuiCategory() {
        return guiCategory;
    }

    public Object getItemOnSlot(int slot) {
        return items.get(slot);
    }

    public void addCategory(GuiCategory category) {
        categories.add(category);
    }

    public void removeCategory(GuiCategory category) {
        categories.remove(category);
    }
}
