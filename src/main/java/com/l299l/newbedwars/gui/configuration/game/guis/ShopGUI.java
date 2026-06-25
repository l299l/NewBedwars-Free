package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.GuiSave;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiCategory;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiUpgrade;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        // Always render header slots (category buttons, fills, etc.)
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

        boolean isHomePage = guiCategory == null || guiCategory.id().isEmpty()
                || guiCategory.id().equalsIgnoreCase("home");

        if (isHomePage) {
            // Determine if fast-buy customization applies (shop GUI only, not upgrade GUI)
            IArena arena = Arena.arenaByWorld.get(getPlayer().getWorld());
            boolean isShopGui = arena != null && guiSave.getGuiId().equals(arena.getShopGuiId());
            com.l299l.newbedwars.player.PlayerIns playerIns = isShopGui
                    ? NewBedwars.plugin.getPlayerManager().getPlayer(getPlayer().getName()) : null;
            Map<String, List<String>> fastBuyPerCat = (isShopGui && playerIns != null)
                    ? playerIns.fastBuyPerCategory() : Map.of();

            if (!fastBuyPerCat.isEmpty()) {
                renderCustomFastBuy(headerSize, fastBuyPerCat);
            } else {
                // Default: render exactly as YAML defines
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
            }

        } else {
            // Category page: render items centered with margins
            int itemsSize = guiCategory.items().size();
            int itemsInLine = 5;
            int marginSize = 2;
            int lines = (contentSize / 9);
            if (itemsInLine * lines < itemsSize) {
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
                if (end) break;
            }
            // Footer area
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

    /**
     * Renders custom per-category fast-buy items onto the home page.
     * Mirrors the YAML default grid: each category occupies a column,
     * rows 1..3 hold fb-slot 1..3 for that category.
     * Empty fast-buy slots render as empty (no item).
     */
    private void renderCustomFastBuy(int headerSize, Map<String, List<String>> fastBuyPerCat) {
        // Collect ordered categories from the header
        List<GuiCategory> orderedCats = new ArrayList<>();
        for (int i = 0; i < headerSize; i++) {
            Object o = guiSave.getItem(i);
            if (o instanceof GuiCategory cat && !cat.id().equalsIgnoreCase("home")) {
                orderedCats.add(cat);
            }
        }
        int numCats = orderedCats.size();
        if (numCats == 0) return;

        // Centre categories in a 9-wide row (same as YAML layout)
        int margin = (9 - Math.min(numCats, 9)) / 2;

        // Up to 3 rows of fast-buy items
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < Math.min(numCats, 9); col++) {
                GuiCategory cat = orderedCats.get(col);
                List<String> catItems = fastBuyPerCat.get(cat.id());
                if (catItems == null || row >= catItems.size()) continue;
                String itemName = catItems.get(row);
                if (itemName == null || itemName.isBlank()) continue;
                CustomItem ci = NewBedwars.plugin.getCustomItemManager().getCustomItem(itemName);
                if (ci == null) continue;
                int slot = headerSize + row * 9 + margin + col;
                if (slot < guiSave.getSize()) {
                    inv.setItem(slot, ci.getIcon(getPlayer()));
                    items.set(slot, ci);
                }
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public List<GuiCategory> getCategories() { return categories; }
    public GuiSave getGuiSave() { return guiSave; }
    public GuiCategory getActualGuiCategory() { return guiCategory; }
    public Object getItemOnSlot(int slot) { return items.get(slot); }
    public void addCategory(GuiCategory category) { categories.add(category); }
    public void removeCategory(GuiCategory category) { categories.remove(category); }
}
