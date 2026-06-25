package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.GuiSave;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiCategory;
import com.l299l.newbedwars.player.PlayerIns;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fast-Buy Customizer — 54-slot inventory.
 *
 * Layout (9 slots per row):
 *   Row 0: header / instructions
 *   Rows 1..ceil(N/2): two categories side-by-side per row
 *     [CatIcon][fb1][fb2][fb3][SEP][CatIcon][fb1][fb2][fb3]
 *   Last row (row 5): padding + Save & Close at slot 53
 *
 * Clicking an fb-slot removes the item from that category slot.
 * Clicking a category icon cycles to the next available item from that category's full item list.
 */
public class FastBuyCustomizerGUI extends BasicGUI {

    /** Maximum fast-buy items per category. */
    public static final int SLOTS_PER_CAT = 3;

    private final Inventory inv;
    private final List<GuiCategory> categories;
    private final Map<String, List<String>> selection; // categoryId → chosen item names

    // Ordered flat list of categories (extracted from header slots of the GuiSave)
    public FastBuyCustomizerGUI(GuiManager guiManager, Player player, GuiSave shopGuiSave) {
        super(guiManager, player);

        // Collect categories from the shop GUI header (skip "home")
        categories = new ArrayList<>();
        int headerSize = (int) shopGuiSave.getGuiData("HeaderSize");
        for (int i = 0; i < headerSize; i++) {
            Object o = shopGuiSave.getItem(i);
            if (o instanceof GuiCategory cat && !cat.id().equalsIgnoreCase("home")) {
                categories.add(cat);
            }
        }

        // Load existing selection
        PlayerIns ins = NewBedwars.plugin.getPlayerManager().getPlayer(player.getName());
        Map<String, List<String>> saved = (ins != null) ? ins.fastBuyPerCategory() : Map.of();
        selection = new HashMap<>();
        for (GuiCategory cat : categories) {
            List<String> saved1 = saved.get(cat.id());
            selection.put(cat.id(), saved1 != null ? new ArrayList<>(saved1) : new ArrayList<>());
        }

        inv = Bukkit.createInventory(this, 54,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Fast-Buy Customizer");
        redraw();
    }

    // ── Slot arithmetic ────────────────────────────────────────────────────────

    /**
     * Row for a category pair: categories 0&1 → row 1, 2&3 → row 2, etc.
     * (row 0 is the header)
     */
    private int rowForPair(int pairIndex) {
        return 1 + pairIndex;
    }

    /** Absolute slot for a given category-pair row, column (0-8). */
    private int slot(int row, int col) {
        return row * 9 + col;
    }

    /**
     * Returns the absolute inventory slot of the given fast-buy slot (0-based)
     * for category at list-index catIndex.
     *
     * Layout within a row:
     *   cols 0-3: left category  [icon, fb0, fb1, fb2]
     *   col  4:  separator
     *   cols 5-8: right category [icon, fb0, fb1, fb2]
     */
    public int fbSlot(int catIndex, int fbIndex) {
        int pairIndex = catIndex / 2;
        int row = rowForPair(pairIndex);
        boolean isRight = (catIndex % 2 == 1);
        int colBase = isRight ? 5 : 0;
        return slot(row, colBase + 1 + fbIndex); // +1 to skip the category icon col
    }

    /** Slot of the category icon for catIndex. */
    private int catIconSlot(int catIndex) {
        int row = rowForPair(catIndex / 2);
        return slot(row, catIndex % 2 == 0 ? 0 : 5);
    }

    /** Returns the category index for an fb-slot click, or -1 if not an fb slot. */
    public int catIndexForSlot(int rawSlot) {
        for (int c = 0; c < categories.size(); c++) {
            for (int f = 0; f < SLOTS_PER_CAT; f++) {
                if (fbSlot(c, f) == rawSlot) return c;
            }
        }
        return -1;
    }

    /** Returns the fb index (0-2) for a click on catIndex, or -1. */
    public int fbIndexForSlot(int catIndex, int rawSlot) {
        for (int f = 0; f < SLOTS_PER_CAT; f++) {
            if (fbSlot(catIndex, f) == rawSlot) return f;
        }
        return -1;
    }

    /** Returns the category index whose icon was clicked, or -1. */
    public int catIndexForIconSlot(int rawSlot) {
        for (int c = 0; c < categories.size(); c++) {
            if (catIconSlot(c) == rawSlot) return c;
        }
        return -1;
    }

    // ── Drawing ────────────────────────────────────────────────────────────────

    public void redraw() {
        // Clear inventory
        inv.clear();

        // Row 0: header pane
        ItemStack header = pane(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Fast-Buy Customizer",
                ChatColor.GRAY + "Click a category icon to add items.",
                ChatColor.GRAY + "Click a filled fast-buy slot to remove.");
        for (int i = 0; i < 9; i++) inv.setItem(i, header);

        // Category rows
        ItemStack sep = pane(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "───");
        for (int c = 0; c < categories.size(); c++) {
            GuiCategory cat = categories.get(c);
            int row = rowForPair(c / 2);
            boolean isRight = (c % 2 == 1);
            int colBase = isRight ? 5 : 0;

            // Separator between the two category halves
            if (!isRight) inv.setItem(slot(row, 4), sep);

            // Category icon
            ItemStack icon = cat.getIcon(getPlayer());
            ItemMeta iconMeta = icon.getItemMeta();
            if (iconMeta != null) {
                List<String> lore = iconMeta.getLore() != null
                        ? new ArrayList<>(iconMeta.getLore()) : new ArrayList<>();
                List<String> current = selection.getOrDefault(cat.id(), List.of());
                lore.add("");
                lore.add(ChatColor.YELLOW + "Fast-buy (" + current.size() + "/" + SLOTS_PER_CAT + "):");
                for (int f = 0; f < SLOTS_PER_CAT; f++) {
                    String item = f < current.size() ? current.get(f) : null;
                    lore.add("  " + (item != null ? ChatColor.GREEN + item : ChatColor.DARK_GRAY + "(empty)"));
                }
                lore.add("");
                lore.add(ChatColor.GRAY + "Click to open item picker for this category.");
                iconMeta.setLore(lore);
                icon.setItemMeta(iconMeta);
            }
            inv.setItem(slot(row, colBase), icon);

            // Fast-buy slots
            List<String> chosen = selection.getOrDefault(cat.id(), List.of());
            for (int f = 0; f < SLOTS_PER_CAT; f++) {
                int s = slot(row, colBase + 1 + f);
                if (f < chosen.size()) {
                    CustomItem ci = NewBedwars.plugin.getCustomItemManager().getCustomItem(chosen.get(f));
                    if (ci != null) {
                        ItemStack it = ci.getIcon(getPlayer());
                        ItemMeta m = it.getItemMeta();
                        if (m != null) {
                            List<String> l = m.getLore() != null ? new ArrayList<>(m.getLore()) : new ArrayList<>();
                            l.add(ChatColor.RED + "Click to remove");
                            m.setLore(l);
                            it.setItemMeta(m);
                        }
                        inv.setItem(s, it);
                    } else {
                        inv.setItem(s, emptySlot(f));
                    }
                } else {
                    inv.setItem(s, emptySlot(f));
                }
            }
        }

        // Row 5 (slots 45-52): separator pane; slot 53: Save
        ItemStack rowSep = pane(Material.GRAY_STAINED_GLASS_PANE, "");
        for (int i = 45; i < 53; i++) inv.setItem(i, rowSep);

        ItemStack save = pane(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Save & Close",
                ChatColor.GRAY + "Saves your fast-buy preferences.");
        inv.setItem(53, save);
    }

    // ── Public API (called by settings listener) ───────────────────────────────

    /** Add itemName to the next empty fb-slot for catIndex. */
    public void addItem(int catIndex, String itemName) {
        if (catIndex < 0 || catIndex >= categories.size()) return;
        GuiCategory cat = categories.get(catIndex);
        List<String> chosen = selection.computeIfAbsent(cat.id(), k -> new ArrayList<>());
        if (!chosen.contains(itemName) && chosen.size() < SLOTS_PER_CAT) {
            chosen.add(itemName);
            redraw();
        }
    }

    /** Returns the category at catIndex, or null if out of range. */
    public GuiCategory getCategory(int catIndex) {
        return (catIndex >= 0 && catIndex < categories.size()) ? categories.get(catIndex) : null;
    }

    /** Returns true if the fb-slot at (catIndex, fbIndex) already has an item. */
    public boolean isSlotFilled(int catIndex, int fbIndex) {
        if (catIndex < 0 || catIndex >= categories.size()) return false;
        List<String> chosen = selection.getOrDefault(categories.get(catIndex).id(), List.of());
        return fbIndex < chosen.size();
    }

    /**
     * Clicking a filled fb-slot: removes that item from the category's selection.
     */
    public void removeFbSlot(int catIndex, int fbIndex) {
        if (catIndex < 0 || catIndex >= categories.size()) return;
        List<String> chosen = selection.computeIfAbsent(categories.get(catIndex).id(), k -> new ArrayList<>());
        if (fbIndex < chosen.size()) {
            chosen.remove(fbIndex);
            redraw();
        }
    }

    /** Returns slot 53. */
    public static int saveSlot() { return 53; }

    public Map<String, List<String>> getSelection() { return selection; }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ItemStack emptySlot(int index) {
        return pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                ChatColor.GRAY + "Slot " + (index + 1) + " — Empty",
                ChatColor.DARK_GRAY + "Click the category icon to add.");
    }

    private static ItemStack pane(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore.length > 0) m.setLore(Arrays.asList(lore));
            item.setItemMeta(m);
        }
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() { return inv; }
}
