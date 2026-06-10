package com.l299l.newbedwars.arena.player.inventory;

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
    private ArmorType armor;
    private CustomItem[] otherArmorContents;
    private CustomItem sword;
    private CustomItem pickaxe;
    private CustomItem axe;
    private CustomItem shears;
    private List<CustomItem> other;

    public ArmorContents(Player player, Team team) {
        this.player = player;
        this.team = team;
        armor = ArmorType.BASIC;
        otherArmorContents = null;
        sword = getBasicSword();
        pickaxe = null;
        axe = null;
        shears = null;
        other = new ArrayList<>();
    }

    public void loadPlayerArmorContents(Player player) {
        this.player = player;
        player.getInventory().setArmorContents(getArmorContents());
        ItemStack[] contents = getInventoryContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                player.getInventory().setItem(i, contents[i]);
            }
        }
    }

    public void setArmor(ArmorType armor) {
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
        other.add(item);
    }

    private ItemStack[] getArmorContents() {
        ChatColor color = team.getColor();
        int protectionLevel = team.getTeamUpgrades().getUpgradeLevel(Upgrade.PROTECTION);
        boolean all = Properties.FullArmor;
        return switch (armor) {
            case GOLD -> getGoldArmor(all, protectionLevel, color);
            case CHAINMAIL -> getChainmailArmor(all, protectionLevel, color);
            case IRON -> getIronArmor(all, protectionLevel, color);
            case DIAMOND -> getDiamondArmor(all, protectionLevel, color);
            case NETHERITE -> getNetheriteArmor(all, protectionLevel, color);
            case OTHER -> getOtherArmorContents(all, protectionLevel, color, player);
            default -> getDefaultArmor(color, protectionLevel);
        };
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
            for (int i = 0; i < other.size(); i++) {
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

    private ItemStack[] getDefaultArmor(ChatColor color, int protectionLevel) {
        ItemStack[] items =  new ItemStack[]{
                createLeatherArmor(Material.LEATHER_BOOTS, color),
                createLeatherArmor(Material.LEATHER_LEGGINGS, color),
                createLeatherArmor(Material.LEATHER_CHESTPLATE, color),
                createLeatherArmor(Material.LEATHER_HELMET, color)
        };
        if (protectionLevel > 0) {
            for (ItemStack item : items) {
                addProtection(item, protectionLevel);
            }
        }
        return items;
    }

    private ItemStack[] getGoldArmor(boolean all, int protectionLevel, ChatColor color) {
        ItemStack[] items;
        if (all) {
            items =  new ItemStack[]{
                    new ItemStack(Material.GOLDEN_BOOTS),
                    new ItemStack(Material.GOLDEN_LEGGINGS),
                    new ItemStack(Material.GOLDEN_CHESTPLATE),
                    new ItemStack(Material.GOLDEN_HELMET)
            };
        } else {
            items = new ItemStack[]{
                    new ItemStack(Material.GOLDEN_BOOTS),
                    new ItemStack(Material.GOLDEN_LEGGINGS),
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

    private ItemStack[] getChainmailArmor(boolean all, int protectionLevel, ChatColor color) {
        ItemStack[] items;
        if (all) {
            items =  new ItemStack[]{
                    new ItemStack(Material.CHAINMAIL_BOOTS),
                    new ItemStack(Material.CHAINMAIL_LEGGINGS),
                    new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                    new ItemStack(Material.CHAINMAIL_HELMET)
            };
        } else {
            items = new ItemStack[]{
                    new ItemStack(Material.CHAINMAIL_BOOTS),
                    new ItemStack(Material.CHAINMAIL_LEGGINGS),
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

    private ItemStack[] getIronArmor(boolean all, int protectionLevel, ChatColor color) {
        ItemStack[] items;
        if (all) {
            items =  new ItemStack[]{
                    new ItemStack(Material.IRON_BOOTS),
                    new ItemStack(Material.IRON_LEGGINGS),
                    new ItemStack(Material.IRON_CHESTPLATE),
                    new ItemStack(Material.IRON_HELMET)
            };
        } else {
            items = new ItemStack[]{
                    new ItemStack(Material.IRON_BOOTS),
                    new ItemStack(Material.IRON_LEGGINGS),
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

    private ItemStack[] getDiamondArmor(boolean all, int protectionLevel, ChatColor color) {
        ItemStack[] items;
        if (all) {
            items =  new ItemStack[]{
                    new ItemStack(Material.DIAMOND_BOOTS),
                    new ItemStack(Material.DIAMOND_LEGGINGS),
                    new ItemStack(Material.DIAMOND_CHESTPLATE),
                    new ItemStack(Material.DIAMOND_HELMET)
            };
        } else {
            items = new ItemStack[]{
                    new ItemStack(Material.DIAMOND_BOOTS),
                    new ItemStack(Material.DIAMOND_LEGGINGS),
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

    private ItemStack[] getNetheriteArmor(boolean all, int protectionLevel, ChatColor color) {
        ItemStack[] items;
        if (all) {
            items =  new ItemStack[]{
                    new ItemStack(Material.NETHERITE_BOOTS),
                    new ItemStack(Material.NETHERITE_LEGGINGS),
                    new ItemStack(Material.NETHERITE_CHESTPLATE),
                    new ItemStack(Material.NETHERITE_HELMET)
            };
        } else {
            items = new ItemStack[]{
                    new ItemStack(Material.NETHERITE_BOOTS),
                    new ItemStack(Material.NETHERITE_LEGGINGS),
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
        meta.setColor(DecoUtils.getColorFromChatColor(color));
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
