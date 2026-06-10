package com.l299l.newbedwars.arena.shops.customitems;

import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.*;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class CustomItemManager {
    private final HashMap<UUID, CustomItem> customItems;
    private final HashMap<String, CustomItem> customItemsByName;
    public CustomItemManager() {
        customItems = new HashMap<>();
        customItemsByName = new HashMap<>();
    }

    public void createDefaults() {
        createCustomItem("WoodenSword", Material.WOODEN_SWORD, 0, PriceType.NONE, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("StoneSword", Material.STONE_SWORD, 10, PriceType.IRON, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("GoldSword", Material.GOLDEN_SWORD, 5, PriceType.GOLD, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("IronSword", Material.IRON_SWORD, 7, PriceType.GOLD, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("DiamondSword", Material.DIAMOND_SWORD, 4, PriceType.EMERALD, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("LeatherArmor", Material.LEATHER_CHESTPLATE, 6, PriceType.IRON, 1, new NoneLogic(), OnBuyEvent.GIVE_ARMOR);
        createCustomItem("ChainmailArmor", Material.CHAINMAIL_CHESTPLATE, 40, PriceType.IRON, 1, new NoneLogic(), OnBuyEvent.GIVE_ARMOR);
        createCustomItem("GoldArmor", Material.GOLDEN_CHESTPLATE, 6, PriceType.GOLD, 1, new NoneLogic(), OnBuyEvent.GIVE_ARMOR);
        createCustomItem("IronArmor", Material.IRON_CHESTPLATE, 12, PriceType.GOLD, 1, new NoneLogic(), OnBuyEvent.GIVE_ARMOR);
        createCustomItem("DiamondArmor", Material.DIAMOND_CHESTPLATE, 6, PriceType.EMERALD, 1, new NoneLogic(), OnBuyEvent.GIVE_ARMOR);
        createCustomItem("WoodenPickaxe", Material.WOODEN_PICKAXE, 10, PriceType.IRON, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("StonePickaxe", Material.STONE_PICKAXE, 5, PriceType.IRON, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("GoldPickaxe", Material.GOLDEN_PICKAXE, 3, PriceType.GOLD, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("IronPickaxe", Material.IRON_PICKAXE, 5, PriceType.GOLD, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("DiamondPickaxe", Material.DIAMOND_PICKAXE, 3, PriceType.EMERALD, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("IronAxe", Material.IRON_AXE, 5, PriceType.GOLD, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("DiamondAxe", Material.DIAMOND_AXE, 3, PriceType.EMERALD, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("Shears", Material.SHEARS, 20, PriceType.IRON, 1, new NoneLogic(), OnBuyEvent.GIVE_TOOL);
        createCustomItem("EndStone", Material.END_STONE, 12, PriceType.IRON, 16);
        createCustomItem("Wood", Material.OAK_PLANKS, 4, PriceType.IRON, 16);
        createCustomItem("Wool", Material.WHITE_WOOL, 4, PriceType.IRON, 16);
        createCustomItem("Sandstone", Material.SANDSTONE, 4, PriceType.IRON, 16);
        createCustomItem("Terracotta", Material.TERRACOTTA, 4, PriceType.IRON, 16);
        createCustomItem("Ladder", Material.LADDER, 4, PriceType.GOLD, 16);
        createCustomItem("Glass", Material.GLASS, 12, PriceType.IRON, 16);
        createCustomItem("Obsidian", Material.OBSIDIAN, 4, PriceType.EMERALD, 4);
        createCustomItem("WaterBucket", Material.WATER_BUCKET, 4, PriceType.GOLD, 1);
        createCustomItem("LavaBucket", Material.LAVA_BUCKET, 3, PriceType.GOLD, 1);
        createCustomItem("TNT", Material.TNT, 4, PriceType.GOLD, 1, new TntLogic(), OnBuyEvent.GIVE_ITEM);
        createCustomItem("Fireball", Material.FIRE_CHARGE, 40, PriceType.IRON, 1, new FireballLogic(), OnBuyEvent.GIVE_ITEM);
        createCustomItem("BlastProtGlass", Material.WHITE_STAINED_GLASS, 4, PriceType.IRON, 4, new BlastProtLogic(), OnBuyEvent.GIVE_ITEM);
        createCustomItem("EnderPearl", Material.ENDER_PEARL, 4, PriceType.EMERALD, 1);
        createCustomItem("BridgeEgg", Material.EGG, 4, PriceType.GOLD, 1, new BridgeEggLogic(), OnBuyEvent.GIVE_ITEM);
        createCustomItem("Bow", Material.BOW, 12, PriceType.GOLD, 1);
        createCustomItem("Arrow", Material.ARROW, 4, PriceType.GOLD, 8);
        HashMap<Enchantment, Integer> enchantments = new HashMap<>();
        enchantments.put(VersionCompat.POWER, 5);
        enchantments.put(VersionCompat.INFINITY, 1);
        createCustomItem("BetterBow", Material.BOW, 24, PriceType.EMERALD, 1, new NoneLogic(), OnBuyEvent.GIVE_ITEM, enchantments);
        createCustomItem("GoldenApple", Material.GOLDEN_APPLE, 4, PriceType.GOLD, 1);
        createCustomItem("Fill", Material.BLACK_STAINED_GLASS_PANE, 0, PriceType.NONE, 1, new NoneLogic(), OnBuyEvent.CANCEL);
        List<ItemFlag> itemFlags = new ArrayList<>();
        itemFlags.add(ItemFlag.HIDE_ENCHANTS);
        createCustomItem("Speed", Material.POTION, 4, PriceType.EMERALD, 1, new PotionLogic(), OnBuyEvent.GIVE_ITEM,
                new HashMap<Enchantment, Integer>() {{put(VersionCompat.GLOW_ENCHANT, 1);}}, itemFlags);
        createCustomItem("Jump", Material.POTION, 4, PriceType.EMERALD, 1, new PotionLogic(), OnBuyEvent.GIVE_ITEM,
                new HashMap<Enchantment, Integer>() {{put(VersionCompat.GLOW_ENCHANT, 1);}}, itemFlags);
        createCustomItem("Inviscibility", Material.POTION, 4, PriceType.EMERALD, 1, new PotionLogic(), OnBuyEvent.GIVE_ITEM,
                new HashMap<Enchantment, Integer>() {{put(VersionCompat.GLOW_ENCHANT, 1);}}, itemFlags);

        createCustomUpdateItem("Sharpness", Material.IRON_SWORD);
        createCustomUpdateItem("Protection", Material.IRON_CHESTPLATE);
        createCustomUpdateItem("Haste", Material.IRON_PICKAXE);
        createCustomUpdateItem("Forge", Material.FURNACE);
        createCustomUpdateItem("HealPool", Material.GOLDEN_APPLE);
        createCustomUpdateItem("AlarmTrap", Material.BELL);
        createCustomUpdateItem("BlindTrap", Material.BLACK_DYE);
        createCustomUpdateItem("MiningFatigueTrap", Material.GOLDEN_PICKAXE);
        createCustomUpdateItem("DragonBuff", Material.DRAGON_HEAD);
        createCustomUpdateItem("CustomTrap", Material.TNT);
        createCustomUpdateItem("CustomUpgrade", Material.BARRIER);
        createCustomUpdateItem("Traps", Material.POTION);
    }
    public void createCustomItem(String name, Material material, int price, PriceType priceType, int amount) {
        createCustomItem(name, material, price, priceType, amount, new NoneLogic(), OnBuyEvent.GIVE_ITEM);
    }

    public void createCustomUpdateItem(String name, Material material) {
    HashMap<Enchantment, Integer> enchantments = new HashMap<>() {{
        put(VersionCompat.GLOW_ENCHANT, 1);
    }};
    List<ItemFlag> itemFlags = new ArrayList<>(){{add(ItemFlag.HIDE_ENCHANTS);}};
        createCustomItem(name, "default", material, -1, PriceType.DIAMOND, 1, "default",
                "default", new NoneLogic(), OnBuyEvent.UPGRADE, true, enchantments, itemFlags);
    }

    public void createCustomItem(String name, Material material, int price, PriceType priceType, int amount, CustomLogic event, OnBuyEvent onBuyEvent) {
        createCustomItem(name, "default", material, price, priceType, amount, "default",
                "default", event, onBuyEvent, false);
    }

    public void createCustomItem(String name, Material material, int price, PriceType priceType, int amount, CustomLogic event, OnBuyEvent onBuyEvent,
                                 HashMap<Enchantment, Integer> enchantments) {
        createCustomItem(name, "default", material, price, priceType, amount, "default",
                "default", event, onBuyEvent, false, enchantments, new ArrayList<>());
    }

    public void createCustomItem(String name, Material material, int price, PriceType priceType, int amount, CustomLogic event, OnBuyEvent onBuyEvent,
                                 HashMap<Enchantment, Integer> enchantments, List<ItemFlag> itemFlags) {
        createCustomItem(name, "default", material, price, priceType, amount, "default",
                "default", event, onBuyEvent, false, enchantments, itemFlags);
    }

    public void createCustomItem(String name, String description, Material material, int price, PriceType priceType, int amount, String iconDesc,
                                 String permission, CustomLogic event, OnBuyEvent onBuyEvent, boolean isPermanent) {
        createCustomItem(name, description, material, price, priceType, amount, iconDesc, permission, event, onBuyEvent, isPermanent, new HashMap<>(), new ArrayList<>());
    }


    public void createCustomItem(String name, String description, Material material, int price, PriceType priceType, int amount, String iconDesc,
                                 String permission, CustomLogic event, OnBuyEvent onBuyEvent, boolean isPermanent, HashMap<Enchantment, Integer> enchantments,
                                 List<ItemFlag> itemFlags) {
        CustomItem customItem = new CustomItem(name, description, material, price, priceType, amount, iconDesc, permission, event, onBuyEvent, isPermanent, enchantments, itemFlags, false);
        customItems.put(customItem.getUuid(), customItem);
        customItemsByName.put(name.toLowerCase(), customItem);
    }

    public void createCustomItem(String name, String description, Material material, int price, PriceType priceType, int amount, String iconDesc,
                                 String permission, LogicType logicType, OnBuyEvent onBuyEvent, boolean isPermanent, HashMap<Enchantment, Integer> enchantments,
                                 List<ItemFlag> itemFlags, boolean load) {
        CustomLogic logic = matchLogicType(logicType);
        CustomItem customItem = new CustomItem(name, description, material, price, priceType, amount, iconDesc, permission, logic, onBuyEvent, isPermanent, enchantments, itemFlags, load);
        customItems.put(customItem.getUuid(), customItem);
        customItemsByName.put(name.toLowerCase(), customItem);
    }
    public void addCustomItem(CustomItem customItem) {
        customItems.put(customItem.getUuid(), customItem);
        customItemsByName.put(customItem.getName().toLowerCase(), customItem);
    }

    public List<CustomItem> getCustomItems() {
        List<CustomItem> customItems = new ArrayList<>();
        for (Map.Entry<UUID, CustomItem> entry : this.customItems.entrySet()) {
            customItems.add(entry.getValue());
        }
        return customItems;
    }

    public HashMap<String, HashMap<String, Object>> getCustomItemsAsHashMap() {
        HashMap<String, HashMap<String, Object>> customItems = new HashMap<>();
        List<CustomItem> customItemsList = getCustomItems();
        for (CustomItem customItem : customItemsList) {
            customItems.put(customItem.getName(), customItem.getAsHashMap());
        }
        return customItems;
    }

    public CustomItem getCustomItem(UUID uuid) {
        return customItems.get(uuid);
    }

    public CustomItem getCustomItem(String name) {
        return customItemsByName.get(name.toLowerCase());
    }

    private CustomLogic matchLogicType(LogicType logicType) {
        return switch (logicType) {
            case TNT -> new TntLogic();
            case FIREBALL -> new FireballLogic();
            case LUCKY_BLOCK -> new LuckyBlockLogic();
            case BRIDGE_EGG -> new BridgeEggLogic();
            case BLAST_PROTECTION -> new BlastProtLogic();
            default -> new NoneLogic();
        };
    }
}
