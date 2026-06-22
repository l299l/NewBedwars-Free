package com.l299l.newbedwars.arena.shops.customitems;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.player.inventory.ArmorType;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.version.VersionCompat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.*;

public class CustomItem {
    private final UUID uuid = UUID.randomUUID();
    private final Messages msg;
    private final GuiManager guiManager;
    private String name;
    private String itemDesc;
    private Material material;
    private int price;
    private PriceType priceType;
    private int amount;
    private String iconDesc;
    private String permission;
    private CustomLogic event;
    private OnBuyEvent onBuyEvent;
    private boolean isPermanent;
    private HashMap<Enchantment, Integer> enchantments;
    private List<ItemFlag> itemFlags;

    public CustomItem(String name, String description, Material material, int price, PriceType priceType, int amount, String iconDesc,
                      String permission, CustomLogic event, OnBuyEvent onBuyEvent, boolean isPermanent, HashMap<Enchantment, Integer> enchantments,
                      List<ItemFlag> itemFlags, boolean load) {
        msg = NewBedwars.plugin.getMessages();
        guiManager = NewBedwars.plugin.getGuiManager();
        this.name = name;
        this.itemDesc = description;
        if (description.equals("default") || description.equals("")) {
            itemDesc = "shop-item-description";
        }else if (!load){
            msg.addCustomItemProperty(name, "-description");
        }
        this.material = material;
        this.price = price;
        this.priceType = priceType;
        this.amount = amount;
        this.iconDesc = iconDesc;
        if (iconDesc.equals("default") || iconDesc.equals("")) {
            this.iconDesc = "shop-itemIcon-description";
        }else if (!load){
            msg.addCustomItemProperty(name, "-icon-description");
        }
        this.permission = permission;
        this.event = event;
        this.onBuyEvent = onBuyEvent;
        this.isPermanent = isPermanent;
        this.enchantments = enchantments;
        this.itemFlags = itemFlags;
        if (!load) {
            msg.addCustomItemProperty(name, "-name");
        }
    }

    public ItemStack getItem(Player player) {
        Language language = NewBedwars.plugin.getPlayerManager().getPlayer(player.getName()).language();
        Material resolvedMaterial = material;
        if (material == Material.WHITE_WOOL) {
            Team team = Arena.arenaByWorld.get(player.getWorld()).getTeam(player);
            resolvedMaterial = getWool(team.getColor());
        }
        ItemStack item = new ItemStack(resolvedMaterial, amount);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        String displayName = msg.getMsg(language, name + "-name");
        meta.setDisplayName(displayName != null ? displayName : name);
        if (msg.getMsg(language, itemDesc) != null && !msg.getMsg(language, itemDesc).equals("")) {
            meta.setLore(Arrays.asList(msg.getMsg(language, itemDesc).split("\n")));
        }
        if(enchantments != null && !enchantments.isEmpty()){
            List<Enchantment> enchants = new ArrayList<>(enchantments.keySet());
            for (Enchantment enchant : enchants) {
                Integer level = enchantments.get(enchant);
                meta.addEnchant(enchant, level, (level > enchant.getMaxLevel()));
            }
        }
        if(itemFlags != null && !itemFlags.isEmpty()){
            for(ItemFlag flag : itemFlags){
                meta.addItemFlags(flag);
            }
        }
        meta.setUnbreakable(isPermanent);
        // Set potion appearance for potion-type items
        if ((material == Material.POTION || material == Material.SPLASH_POTION) && meta instanceof PotionMeta potionMeta) {
            PotionType pt = getPotionTypeByName(name);
            VersionCompat.applyPotionType(potionMeta, pt);
        }
        NamespacedKey key = new NamespacedKey(NewBedwars.plugin, "custom_item_name");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        return item;
    }

    private static PotionType getPotionTypeByName(String name) {
        return switch (name.toLowerCase()) {
            case "speed"         -> VersionCompat.POTION_SPEED;
            case "jump"          -> VersionCompat.POTION_JUMP;
            case "inviscibility" -> VersionCompat.POTION_INVISIBILITY;
            default              -> null;
        };
    }

    public boolean buyItem(Player player) {
        if (onBuyEvent == OnBuyEvent.CANCEL) {
            return false;
        }
        if (permission != null && !permission.isEmpty() && !permission.equalsIgnoreCase("default") && !player.hasPermission(permission)) {
            return false;
        }
        if (onBuyEvent == OnBuyEvent.GIVE_ARMOR) {
            ArmorContents armorContents = Arena.arenaByWorld.get(player.getWorld()).getTeam(player).getArmorContents(player);
            ArmorType newType = ArmorType.getArmorType(material);
            if (newType != ArmorType.OTHER && armorContents.getArmor().ordinal() >= newType.ordinal()) {
                return false;
            }
        }
        if (priceType == PriceType.EMERALD) {
            if (player.getInventory().contains(Material.EMERALD, price)) {
                player.getInventory().removeItem(new ItemStack(Material.EMERALD, price));
            }else {
                return false;
            }
        }else if (priceType == PriceType.DIAMOND) {
            if (player.getInventory().contains(Material.DIAMOND, price)) {
                player.getInventory().removeItem(new ItemStack(Material.DIAMOND, price));
            }else {
                return false;
            }
        }else if (priceType == PriceType.GOLD) {
            if (player.getInventory().contains(Material.GOLD_INGOT, price)) {
                player.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, price));
            }else {
                return false;
            }
        }else if (priceType == PriceType.IRON) {
            if (player.getInventory().contains(Material.IRON_INGOT, price)) {
                player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, price));
            }else {
                return false;
            }
        }
        if (onBuyEvent == OnBuyEvent.UPGRADE) {
            return buyUpgrade(player);
        }
        switch (onBuyEvent) {
            case CANCEL, COMMANDS, OPEN_GUI, GIVE_PERM -> {
                return false;
            }
            case GIVE_ITEM -> {
                player.getInventory().addItem(getItem(player))  ;
                if (isPermanent) {
                    Arena.arenaByWorld.get(player.getWorld()).getTeam(player).getArmorContents(player).addOther(this);
                }
                return true;
            }
            case GIVE_ARMOR -> {
                ArmorContents armorContents = Arena.arenaByWorld.get(player.getWorld()).getTeam(player).getArmorContents(player);
                armorContents.setArmor(ArmorType.getArmorType(material));
                armorContents.loadPlayerArmorContents(player);
                return true;
            }
            case GIVE_TOOL -> {
                Team toolTeam = Arena.arenaByWorld.get(player.getWorld()).getTeam(player);
                ArmorContents armorContents = toolTeam.getArmorContents(player);
                boolean isSword = material.name().endsWith("_SWORD");
                if (isSword && armorContents.hasBoughtSword()) {
                    // Player already owns a non-default sword — add this one as an extra item
                    armorContents.setTool(this);
                    player.getInventory().addItem(getItem(player));
                } else {
                    // First sword purchase or non-sword tool — replace the current slot immediately
                    armorContents.setTool(this);
                    armorContents.loadPlayerArmorContents(player);
                }
                // Apply team upgrades (sharpness, haste) to the newly placed sword
                if (isSword) {
                    toolTeam.getTeamUpgrades().applyPlayerUpgrades(player);
                }
                return true;
            }
        }
        return false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return itemDesc;
    }

    public void setDescription(String description) {
        this.itemDesc = description;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public ItemStack getIcon(Language language) {
        return buildIcon(language, material);
    }

    public ItemStack getIcon(Player player) {
        Language language = NewBedwars.plugin.getPlayerManager().getPlayer(player.getName()).language();
        Material iconMaterial = material;
        if (material == Material.WHITE_WOOL) {
            IArena arena = Arena.arenaByWorld.get(player.getWorld());
            if (arena != null) {
                Team team = arena.getTeam(player);
                if (team != null) iconMaterial = getWool(team.getColor());
            }
        }
        return buildIcon(language, iconMaterial);
    }

    private ItemStack buildIcon(Language language, Material iconMaterial) {
        // Try per-item description first, fall back to the shared template
        String desc = msg.getMsg(language, name + "-icon-description");
        if (desc == null || desc.isEmpty()) {
            desc = msg.getMsg(language, iconDesc);
        }
        if (desc == null) desc = "";
        desc = desc.replace("/price/", price + " " + priceType.name());
        desc = desc.replace("/amount/", String.valueOf(amount));
        String displayName = msg.getMsg(language, name + "-name");
        if (displayName == null) displayName = name;
        desc = desc.replace("/name/", displayName);
        return guiManager.createIcon(iconMaterial.name(), displayName, desc, (enchantments != null && !enchantments.isEmpty()));
    }

    public void setIconDesc(String iconDesc) {
        this.iconDesc = iconDesc;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public CustomLogic getEvent() {
        return event;
    }

    public void setEvent(CustomLogic event) {
        this.event = event;
    }

    public OnBuyEvent getOnBuyEvent() {
        return onBuyEvent;
    }

    public void setOnBuyEvent(OnBuyEvent onBuyEvent) {
        this.onBuyEvent = onBuyEvent;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public void setPermanent(boolean permanent) {
        isPermanent = permanent;
    }

    public HashMap<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(HashMap<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public void addEnchantment(Enchantment enchantment, Integer level){
        enchantments.put(enchantment, level);
    }

    public List<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    public void setItemFlags(List<ItemFlag> itemFlags) {
        this.itemFlags = itemFlags;
    }

    public void addItemFlag(ItemFlag itemFlag){
        itemFlags.add(itemFlag);
    }

    public HashMap<String, Object> getAsHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("description", itemDesc);
        map.put("material", material.name());
        map.put("price", price);
        map.put("priceType", priceType.name());
        map.put("amount", amount);
        map.put("iconDesc", iconDesc);
        map.put("permission", permission);
        map.put("itemLogic", event.getType().name());
        map.put("onBuyEvent", onBuyEvent.name());
        map.put("isPermanent", isPermanent);
        map.put("enchantments", getEnchantmentsAsHashMap());
        map.put("itemFlags", getItemFlagsAsHashMap());
        return map;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(name).append("\",");
        sb.append("\"description\":\"").append(itemDesc).append("\",");
        sb.append("\"material\":\"").append(material).append("\",");
        sb.append("\"price\":").append(price).append(",");
        sb.append("\"priceType\":\"").append(priceType.name()).append("\",");
        sb.append("\"amount\":").append(amount).append(",");
        sb.append("\"iconDesc\":\"").append(iconDesc).append("\",");
        sb.append("\"permission\":\"").append(permission).append("\",");
        sb.append("\"event\":\"").append(event.getType().name()).append("\",");
        sb.append("\"onBuyEvent\":\"").append(onBuyEvent.name()).append("\",");
        sb.append("\"isPermanent\":").append(isPermanent).append(",");
        sb.append("\"enchantments\":{");
        if(enchantments != null && !enchantments.isEmpty()){
            List<Enchantment> enchants = new ArrayList<>(enchantments.keySet());
            for (int i = 0; i < enchants.size(); i++) {
                Enchantment enchant = enchants.get(i);
                Integer level = enchantments.get(enchant);
                sb.append("\"").append(enchant).append("\":").append(level);
                if(i < enchants.size() - 1){
                    sb.append(",");
                }
            }
        }
        sb.append("},");
        sb.append("\"itemFlags\":[");
        if(itemFlags != null && !itemFlags.isEmpty()){
            for (int i = 0; i < itemFlags.size(); i++) {
                ItemFlag flag = itemFlags.get(i);
                sb.append("\"").append(flag).append("\"");
                if(i < itemFlags.size() - 1){
                    sb.append(",");
                }
            }
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private boolean buyUpgrade(Player p) {
        Upgrade upgrade = Upgrade.valueOf(name.toUpperCase());
        Team team = Arena.arenaByWorld.get(p.getWorld()).getTeam(p);
        if (team.getTeamUpgrades().getUpgradeLevel(upgrade) >= upgrade.maxLevel) {
            return false;
        }
        team.getTeamUpgrades().upgrade(upgrade);
        return true;
    }

    private HashMap<String, Integer> getEnchantmentsAsHashMap() {
        HashMap<String, Integer> map = new HashMap<>();
        for (Enchantment enchantment : enchantments.keySet()) {
            map.put(enchantment.getKey().getKey(), enchantments.get(enchantment));
        }
        return map;
    }

    private List<String> getItemFlagsAsHashMap() {
        List<String> list = new ArrayList<>();
        for (ItemFlag itemFlag : itemFlags) {
            list.add(itemFlag.name());
        }
        return list;
    }

    private Material getWool(ChatColor color) {
        switch (color) {
            case RED, DARK_RED -> {
                return Material.RED_WOOL;
            }
            case BLUE -> {
                return Material.BLUE_WOOL;
            }
            case GREEN, DARK_GREEN -> {
                return Material.GREEN_WOOL;
            }
            case YELLOW -> {
                return Material.YELLOW_WOOL;
            }
            case AQUA -> {
                return Material.LIGHT_BLUE_WOOL;
            }
            case BLACK -> {
                return Material.BLACK_WOOL;
            }
            case DARK_GRAY -> {
                return Material.GRAY_WOOL;
            }
            case GRAY -> {
                return Material.LIGHT_GRAY_WOOL;
            }
            case DARK_AQUA -> {
                return Material.CYAN_WOOL;
            }
            case DARK_PURPLE -> {
                return Material.PURPLE_WOOL;
            }
            case GOLD -> {
                return Material.ORANGE_WOOL;
            }
            case LIGHT_PURPLE -> {
                return Material.MAGENTA_WOOL;
            }
            default -> {
                return Material.WHITE_WOOL;
            }
        }
    }
}

