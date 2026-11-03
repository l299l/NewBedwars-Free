package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.gui.model.GUIEntry;

import com.l299l.newbedwars.gui.ItemBuilder;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GUIManager;
import com.l299l.newbedwars.gui.GUISave;
import com.l299l.newbedwars.gui.model.GUICategory;
import com.l299l.newbedwars.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FastBuyCustomizerGUI extends BasicGUI {

    public static final int SLOTS_PER_CAT = 3;

    private static final int MAX_CATEGORIES = 8;

    private final Inventory inv;
    private final List<GUICategory> categories;
    private final Map<String, List<String>> selection;

    public FastBuyCustomizerGUI(GUIManager guiManager, Player player, GUISave shopGuiSave) {
        super(guiManager, player);

        categories = new ArrayList<>();
        int headerSize = shopGuiSave.getHeaderSize();
        for (int i = 0; i < headerSize; i++) {
            GUIEntry o = shopGuiSave.getItem(i);
            if (o instanceof GUICategory cat && !cat.id().equalsIgnoreCase("home") && categories.size() < MAX_CATEGORIES) {
                categories.add(cat);
            }
        }

        PlayerProfile ins = NewBedwars.plugin.getPlayerManager().getPlayer(player.getName());
        Map<String, List<String>> saved = (ins != null) ? ins.fastBuyPerCategory() : Map.of();
        selection = new HashMap<>();
        for (GUICategory cat : categories) {
            List<String> saved1 = saved.get(cat.id());
            selection.put(cat.id(), saved1 != null ? new ArrayList<>(saved1) : new ArrayList<>());
        }

        inv = Bukkit.createInventory(this, 54,
                ChatColor.AQUA + "" + ChatColor.BOLD + text("GuiFastBuyTitle"));
        redraw();
    }

    private int rowForPair(int pairIndex) {
        return 1 + pairIndex;
    }

    private int slot(int row, int col) {
        return row * 9 + col;
    }

    public int fbSlot(int catIndex, int fbIndex) {
        int pairIndex = catIndex / 2;
        int row = rowForPair(pairIndex);
        boolean isRight = (catIndex % 2 == 1);
        int colBase = isRight ? 5 : 0;
        return slot(row, colBase + 1 + fbIndex);
    }

    private int catIconSlot(int catIndex) {
        int row = rowForPair(catIndex / 2);
        return slot(row, catIndex % 2 == 0 ? 0 : 5);
    }

    public int catIndexForSlot(int rawSlot) {
        for (int c = 0; c < categories.size(); c++) {
            for (int f = 0; f < SLOTS_PER_CAT; f++) {
                if (fbSlot(c, f) == rawSlot) return c;
            }
        }
        return -1;
    }

    public int fbIndexForSlot(int catIndex, int rawSlot) {
        for (int f = 0; f < SLOTS_PER_CAT; f++) {
            if (fbSlot(catIndex, f) == rawSlot) return f;
        }
        return -1;
    }

    public int catIndexForIconSlot(int rawSlot) {
        for (int c = 0; c < categories.size(); c++) {
            if (catIconSlot(c) == rawSlot) return c;
        }
        return -1;
    }

    public void redraw() {
        inv.clear();

        ItemStack header = pane(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.AQUA + "" + ChatColor.BOLD + text("GuiFastBuyTitle"),
                ChatColor.GRAY + text("GuiFastBuyInfo1"),
                ChatColor.GRAY + text("GuiFastBuyInfo2"));
        for (int i = 0; i < 9; i++) inv.setItem(i, header);

        ItemStack sep = pane(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "───");
        for (int c = 0; c < categories.size(); c++) {
            GUICategory cat = categories.get(c);
            int row = rowForPair(c / 2);
            boolean isRight = (c % 2 == 1);
            int colBase = isRight ? 5 : 0;

            if (!isRight) inv.setItem(slot(row, 4), sep);

            ItemStack icon = cat.getIcon(getPlayer());
            ItemMeta iconMeta = icon.getItemMeta();
            if (iconMeta != null) {
                List<String> lore = iconMeta.getLore() != null
                        ? new ArrayList<>(iconMeta.getLore()) : new ArrayList<>();
                List<String> current = selection.getOrDefault(cat.id(), List.of());
                lore.add("");
                lore.add(ChatColor.YELLOW + text("GuiFastBuyCount").replace("/count/", String.valueOf(current.size())).replace("/max/", String.valueOf(SLOTS_PER_CAT)));
                for (int f = 0; f < SLOTS_PER_CAT; f++) {
                    String item = f < current.size() ? current.get(f) : null;
                    lore.add("  " + (item != null ? ChatColor.GREEN + item : ChatColor.DARK_GRAY + text("GuiFastBuyEmpty")));
                }
                lore.add("");
                lore.add(ChatColor.GRAY + text("GuiFastBuyOpenPicker"));
                iconMeta.setLore(lore);
                icon.setItemMeta(iconMeta);
            }
            inv.setItem(slot(row, colBase), icon);

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
                            l.add(ChatColor.RED + text("GuiClickRemove"));
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

        ItemStack rowSep = pane(Material.GRAY_STAINED_GLASS_PANE, "");
        for (int i = 45; i < 53; i++) inv.setItem(i, rowSep);

        ItemStack reset = pane(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "" + ChatColor.BOLD + text("GuiFastBuyReset"),
                ChatColor.GRAY + text("GuiFastBuyResetInfo1"),
                ChatColor.GRAY + text("GuiFastBuyResetInfo2"));
        inv.setItem(resetSlot(), reset);

        ItemStack save = pane(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + text("GuiFastBuySave"),
                ChatColor.GRAY + text("GuiFastBuySaveInfo"));
        inv.setItem(53, save);
    }

    public void addItem(int catIndex, String itemName) {
        if (catIndex < 0 || catIndex >= categories.size()) return;
        GUICategory cat = categories.get(catIndex);
        List<String> chosen = selection.computeIfAbsent(cat.id(), k -> new ArrayList<>());
        if (!chosen.contains(itemName) && chosen.size() < SLOTS_PER_CAT) {
            chosen.add(itemName);
            redraw();
        }
    }

    public GUICategory getCategory(int catIndex) {
        return (catIndex >= 0 && catIndex < categories.size()) ? categories.get(catIndex) : null;
    }

    public boolean isSlotFilled(int catIndex, int fbIndex) {
        if (catIndex < 0 || catIndex >= categories.size()) return false;
        List<String> chosen = selection.getOrDefault(categories.get(catIndex).id(), List.of());
        return fbIndex < chosen.size();
    }

    public void removeFbSlot(int catIndex, int fbIndex) {
        if (catIndex < 0 || catIndex >= categories.size()) return;
        List<String> chosen = selection.computeIfAbsent(categories.get(catIndex).id(), k -> new ArrayList<>());
        if (fbIndex < chosen.size()) {
            chosen.remove(fbIndex);
            redraw();
        }
    }

    public void resetAll() {
        selection.clear();
        redraw();
    }

    public static int saveSlot() { return 53; }

    public static int resetSlot() { return 49; }

    public Map<String, List<String>> getSelection() {
        Map<String, List<String>> persisted = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : selection.entrySet()) {
            if (!entry.getValue().isEmpty()) persisted.put(entry.getKey(), entry.getValue());
        }
        return persisted;
    }

    private ItemStack emptySlot(int index) {
        return pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                ChatColor.GRAY + text("GuiFastBuySlotEmpty").replace("/slot/", String.valueOf(index + 1)),
                ChatColor.DARK_GRAY + text("GuiFastBuySlotEmptyInfo"));
    }

    private static ItemStack pane(Material mat, String name, String... lore) {
        return ItemBuilder.of(mat).name(name).lore(lore).build();
    }

    @Override
    public @NotNull Inventory getInventory() { return inv; }
}
