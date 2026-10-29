package com.l299l.newbedwars.arena.player.inventory;

import com.l299l.newbedwars.utils.TeamColor;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.utils.DecoUtils;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

public class ArmorContents {
    private final Team team;
    private Player player;
    private ArmorTier armor;
    private CustomItem[] otherArmorContents;
    private CustomItem sword;
    private CustomItem pickaxe;
    private CustomItem axe;
    private CustomItem shears;
    private List<CustomItem> other;

    public ArmorContents(Player player, Team team) {
        this.player = player;
        this.team = team;
        armor = ArmorTier.BASIC;
        otherArmorContents = null;
        sword = getBasicSword();
        pickaxe = null;
        axe = null;
        shears = null;
        other = new ArrayList<>();
    }

    /** Equips the armor only; the rest of the inventory is left alone. */
    public void applyArmor(Player player) {
        this.player = player;
        ItemStack[] armorToSet = getArmorContents();
        IArena armorArena = Arena.arenaByWorld.get(player.getWorld());
        if (armorArena != null) armorToSet = ApiEvents.armorEquip(armorArena, player, armorToSet);
        player.getInventory().setArmorContents(armorToSet);
    }

    /**
     * Gives a freshly bought tool. It takes the place of the player's current tool of the same kind
     * (wooden pickaxe -> iron pickaxe) wherever they moved it; other slots are never touched.
     */
    public void giveTool(Player player, CustomItem tool) {
        this.player = player;
        String type = getToolType(tool.getMaterial());
        ItemStack newItem = tool.getItem(player);
        if (!type.equals("OTHER")) {
            ItemStack[] storage = player.getInventory().getStorageContents();
            int target = -1;
            for (int i = 0; i < storage.length; i++) {
                if (storage[i] == null || !getToolType(storage[i].getType()).equals(type)) continue;
                if (target == -1) {
                    target = i;
                } else {
                    player.getInventory().setItem(i, null);
                }
            }
            if (target != -1) {
                player.getInventory().setItem(target, newItem);
                return;
            }
        }
        giveOrDrop(player, newItem);
    }

    /** Adds the item to the inventory; what does not fit lands at the player's feet instead of vanishing. */
    public static void giveOrDrop(Player player, ItemStack item) {
        for (ItemStack left : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    /** Equips everything (start of the game, respawn). */
    public void loadPlayerArmorContents(Player player) {
        applyArmor(player);
        ItemStack[] contents = getInventoryContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                player.getInventory().setItem(i, contents[i]);
            }
        }
    }

    public ArmorTier getArmor() {
        return armor;
    }

    public void setArmor(ArmorTier armor) {
        this.armor = armor;
    }

    public void setOtherArmorContents(CustomItem[] otherArmorContents) {
        this.otherArmorContents = otherArmorContents;
    }

    public void setTool(CustomItem tool) {
        switch (getToolType(tool.getMaterial())) {
            case "SWORD" -> sword = tool;
            case "PICKAXE" -> pickaxe = tool;
            case "AXE" -> axe = tool;
            case "SHEARS" -> shears = tool;
            default -> other.add(tool);
        }
    }

    public void resetSword() {
        sword = getBasicSword();
    }

    public boolean hasBoughtSword() {
        if (sword == null) return false;
        CustomItem basic = getBasicSword();
        return basic == null || !sword.getName().equalsIgnoreCase(basic.getName());
    }

    public void setOther(List<CustomItem> other) {
        this.other = other;
    }
    public void setOther(int index, CustomItem item) {
        other.set(index, item);
    }
    public void addOther(CustomItem item) {
        for (CustomItem owned : other) {
            if (owned.getName().equalsIgnoreCase(item.getName())) return;
        }
        other.add(item);
    }

    private ItemStack[] getArmorContents() {
        ChatColor color = team.getColor();
        int protectionLevel = team.getTeamUpgrades().getUpgradeLevel(Upgrade.PROTECTION);
        boolean all = Properties.FullArmor;
        if (armor == ArmorTier.OTHER && otherArmorContents != null) return getOtherArmorContents(all, protectionLevel, color, player);
        if (armor == ArmorTier.OTHER) return getTierArmor(ArmorTier.BASIC, all, protectionLevel, color);
        return getTierArmor(armor, all, protectionLevel, color);
    }

    private ItemStack[] getInventoryContents() {
        ItemStack[] items = new ItemStack[36];
        items[0] = sword.getItem(player);
        if (pickaxe != null) {
            items[1] = pickaxe.getItem(player);
        }
        if (axe != null) {
            items[2] = axe.getItem(player);
        }
        if (shears != null) {
            items[3] = shears.getItem(player);
        }
        if (other != null) {
            for (int i = 0; i < other.size() && i + 4 < items.length; i++) {
                items[i + 4] = other.get(i).getItem(player);
            }
        }
        return items;
    }

    private String getToolType(Material material) {
        switch (material) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> {
                return "SWORD";
            }
            case WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE -> {
                return "PICKAXE";
            }
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> {
                return "AXE";
            }
            case SHEARS -> {
                return "SHEARS";
            }
            default -> {
                return "OTHER";
            }
        }
    }

    private CustomItem getBasicSword() {
        return NewBedwars.plugin.getCustomItemManager().getCustomItem(Properties.BasicSword);
    }

    /** Boots and leggings of the tier, plus its chestplate and helmet when {@code FullArmor} is on (else team leather). */
    private ItemStack[] getTierArmor(ArmorTier tier, boolean all, int protectionLevel, ChatColor color) {
        ItemStack[] items = new ItemStack[]{
                piece(tier.boots(), color),
                piece(tier.leggings(), color),
                all ? piece(tier.chestplate(), color) : createLeatherArmor(Material.LEATHER_CHESTPLATE, color),
                all ? piece(tier.helmet(), color) : createLeatherArmor(Material.LEATHER_HELMET, color)
        };
        if (protectionLevel > 0) {
            for (ItemStack item : items) {
                addProtection(item, protectionLevel);
            }
        }
        return items;
    }

    /** Leather pieces get the team color, everything else is used as is. */
    private ItemStack piece(Material material, ChatColor color) {
        return material.name().startsWith("LEATHER_") ? createLeatherArmor(material, color) : new ItemStack(material);
    }

    private ItemStack[] getOtherArmorContents(boolean all, int protectionLevel, ChatColor color, Player player) {
        ItemStack[] items;
        NewBedwars plugin = NewBedwars.plugin;
        if (all) {
            items =  new ItemStack[]{
                    new ItemStack(otherArmorContents[0].getItem(player)),
                    new ItemStack(otherArmorContents[1].getItem(player)),
                    new ItemStack(otherArmorContents[2].getItem(player)),
                    new ItemStack(otherArmorContents[3].getItem(player))
            };
        } else {
            items = new ItemStack[]{
                    new ItemStack(otherArmorContents[0].getItem(player)),
                    new ItemStack(otherArmorContents[1].getItem(player)),
                    createLeatherArmor(Material.LEATHER_CHESTPLATE, color),
                    createLeatherArmor(Material.LEATHER_HELMET, color)
            };
        }
        if (protectionLevel > 0) {
            for (ItemStack item : items) {
                addProtection(item, protectionLevel);
            }
        }
        return items;
    }

    private ItemStack createLeatherArmor(Material material, ChatColor color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(TeamColor.leather(color));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack addProtection(ItemStack item, int protectionLevel) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            VersionCompat.addProtection(meta, protectionLevel);
            item.setItemMeta(meta);
        }
        return item;
    }
}
