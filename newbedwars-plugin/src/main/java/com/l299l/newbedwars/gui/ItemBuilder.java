package com.l299l.newbedwars.gui;

import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/** Builds GUI icons: {@code ItemBuilder.of(Material.STONE).name("Stone").lore("a", "b").build()}. */
public final class ItemBuilder {
    private final ItemStack item;
    private String name;
    private List<String> lore;
    private boolean glow;

    private ItemBuilder(Material material, int amount) {
        item = new ItemStack(material, amount);
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material, 1);
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    public ItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    /** Sets the lore; no lines leaves the lore empty. */
    public ItemBuilder lore(String... lines) {
        this.lore = lines.length == 0 ? null : Arrays.asList(lines);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        this.lore = lines;
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (name != null) meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        if (glow) VersionCompat.addGlowEffect(meta);
        item.setItemMeta(meta);
        return item;
    }
}
