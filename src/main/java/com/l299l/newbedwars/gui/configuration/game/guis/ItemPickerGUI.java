package com.l299l.newbedwars.gui.configuration.game.guis;

import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.gui.BasicGUI;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.configuration.game.guis.other.GuiCategory;
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
import java.util.List;

public class ItemPickerGUI extends BasicGUI {
    public static final int BACK_SLOT = 35;

    private final Inventory inv;
    private final FastBuyCustomizerGUI parent;
    private final int catIndex;
    private final List<CustomItem> available;

    public ItemPickerGUI(GuiManager guiManager, Player player, FastBuyCustomizerGUI parent, int catIndex) {
        super(guiManager, player);
        this.parent = parent;
        this.catIndex = catIndex;

        GuiCategory cat = parent.getCategory(catIndex);
        List<String> already = parent.getSelection().getOrDefault(cat.id(), List.of());

        available = new ArrayList<>();
        for (Object o : cat.items()) {
            if (o instanceof CustomItem ci && !already.contains(ci.getName())) {
                available.add(ci);
            }
        }

        String catDisplayName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cat.name()));
        inv = Bukkit.createInventory(this, 36, ChatColor.YELLOW + "Pick for " + catDisplayName);
        draw();
    }

    private void draw() {
        inv.clear();
        for (int i = 0; i < available.size() && i < BACK_SLOT; i++) {
            ItemStack icon = available.get(i).getIcon(getPlayer());
            ItemMeta m = icon.getItemMeta();
            if (m != null) {
                List<String> lore = m.getLore() != null ? new ArrayList<>(m.getLore()) : new ArrayList<>();
                lore.add(ChatColor.GREEN + "Click to add to fast-buy");
                m.setLore(lore);
                icon.setItemMeta(m);
            }
            inv.setItem(i, icon);
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta m = back.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.RED + "Back");
            m.setLore(Arrays.asList(ChatColor.GRAY + "Return to the customizer"));
            back.setItemMeta(m);
        }
        inv.setItem(BACK_SLOT, back);
    }

    public FastBuyCustomizerGUI getParent() { return parent; }
    public int getCatIndex() { return catIndex; }

    public CustomItem getItemAt(int slot) {
        return (slot >= 0 && slot < available.size()) ? available.get(slot) : null;
    }

    @Override
    public @NotNull Inventory getInventory() { return inv; }
}
