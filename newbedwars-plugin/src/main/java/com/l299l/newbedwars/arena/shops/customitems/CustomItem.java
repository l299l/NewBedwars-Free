package com.l299l.newbedwars.arena.shops.customitems;

import com.l299l.newbedwars.gui.model.GUIEntry;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.utils.TeamColor;
import com.l299l.newbedwars.api.events.ShopPurchaseEvent;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.player.inventory.ArmorTier;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.upgrades.UpgradePurchase;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.GUIManager;
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
import java.util.stream.Collectors;

public class CustomItem implements GUIEntry {
    private static final java.util.regex.Pattern SAFE_NAME = java.util.regex.Pattern.compile("[A-Za-z0-9_.]{1,32}");
    private final UUID uuid = UUID.randomUUID();
    private final Messages msg;
    private final GUIManager guiManager;
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
    private List<String> commands = new ArrayList<>();
    private String openGui = "";
    private String givePermission = "";
    private Material otherPriceMaterial;

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
        return getItem(player, amount);
    }

    public ItemStack getItem(Player player, int stackAmount) {
        Language language = NewBedwars.plugin.getPlayerManager().getPlayer(player.getName()).language();
        Material resolvedMaterial = material;
        if (TeamColor.undyedKind(material) != null) {
            Team team = ArenaRegistry.byWorld(player.getWorld()).getTeam(player);
            resolvedMaterial = TeamColor.recolor(material, team.getColor());
        }
        ItemStack item = new ItemStack(resolvedMaterial, Math.max(1, stackAmount));
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        String displayName = msg.getMsg(language, name + "-name");
        meta.setDisplayName(displayName != null ? displayName : name);
        String lore = msg.getMsg(language, itemDesc);
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(Arrays.asList(lore.split("\n")));
        }
        if (enchantments != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), entry.getValue() > entry.getKey().getMaxLevel());
            }
        }
        if (itemFlags != null && !itemFlags.isEmpty()) {
            meta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));
        }
        meta.setUnbreakable(isPermanent);

        if ((material == Material.POTION || material == Material.SPLASH_POTION) && meta instanceof PotionMeta potionMeta) {
            PotionType pt = getPotionTypeByName(name);
            VersionCompat.applyPotionType(potionMeta, pt);
        }
        NamespacedKey key = new NamespacedKey(NewBedwars.plugin, "custom_item_name");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        return ApiEvents.shopItemStack(player, name, com.l299l.newbedwars.api.events.ShopItemStackEvent.Use.GIVEN, item);
    }

    private static PotionType getPotionTypeByName(String name) {
        return switch (name.toLowerCase()) {
            case "speed"         -> VersionCompat.POTION_SPEED;
            case "jump"          -> VersionCompat.POTION_JUMP;
            case "inviscibility" -> VersionCompat.POTION_INVISIBILITY;
            default              -> null;
        };
    }

    public PurchaseResult buyItem(Player player) {
        return buyItem(player, (ItemPricing.Offer) null);
    }

    public PurchaseResult buyItem(Player player, ItemPricing.Offer offer) {
        if (onBuyEvent == OnBuyEvent.CANCEL) {
            return PurchaseResult.CANCELLED;
        }
        if (permission != null && !permission.isEmpty() && !permission.equalsIgnoreCase("default") && !player.hasPermission(permission)) {
            return PurchaseResult.NO_PERMISSION;
        }
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        if (!UpgradePurchase.canBuy(arena, player)) {
            return PurchaseResult.UNAVAILABLE;
        }
        Team team = arena.getTeam(player);
        boolean needsTeam = onBuyEvent == OnBuyEvent.GIVE_ARMOR || onBuyEvent == OnBuyEvent.GIVE_TOOL || onBuyEvent == OnBuyEvent.UPGRADE;
        if (needsTeam && team == null) {
            return PurchaseResult.UNAVAILABLE;
        }
        if (onBuyEvent == OnBuyEvent.UPGRADE) {
            Upgrade upgrade;
            try {
                upgrade = Upgrade.valueOf(name);
            } catch (IllegalArgumentException e) {
                return PurchaseResult.UNAVAILABLE;
            }
            if (team.getTeamUpgrades() == null) return PurchaseResult.UNAVAILABLE;
            return switch (UpgradePurchase.buy(player, arena, team, upgrade, name).outcome()) {
                case BOUGHT -> PurchaseResult.BOUGHT;
                case MAXED -> PurchaseResult.MAXED;
                case NOT_ENOUGH -> PurchaseResult.NOT_ENOUGH;
                case CANCELLED -> PurchaseResult.CANCELLED;
                case UNAVAILABLE -> PurchaseResult.UNAVAILABLE;
            };
        }
        if (onBuyEvent == OnBuyEvent.GIVE_ARMOR) {
            ArmorTier newType = ArmorTier.getArmorTier(material);

            if (newType == ArmorTier.OTHER) {
                return PurchaseResult.UNAVAILABLE;
            }
            if (team.getArmorContents(player).getArmor().rank() >= newType.rank()) {
                return PurchaseResult.ALREADY_OWNED;
            }
        }
        if (onBuyEvent == OnBuyEvent.COMMANDS && commands.isEmpty()) {
            return PurchaseResult.UNAVAILABLE;
        }
        if (onBuyEvent == OnBuyEvent.OPEN_GUI && guiManager.getGui(openGui) == null) {
            return PurchaseResult.UNAVAILABLE;
        }
        if (onBuyEvent == OnBuyEvent.GIVE_PERM && (arena == null || givePermission.isEmpty())) {
            return PurchaseResult.UNAVAILABLE;
        }
        if (onBuyEvent == OnBuyEvent.GIVE_PERM && GamePermissions.has(player.getUniqueId(), givePermission)) {
            return PurchaseResult.ALREADY_OWNED;
        }
        if (offer == null) offer = ItemPricing.offer(this, null, arena);
        if (offer.priceType() == PriceType.OTHER && offer.otherMaterial() == null) {
            return PurchaseResult.UNAVAILABLE;
        }
        Material priceMaterial = offer.priceMaterial();
        if (priceMaterial != null && !player.getInventory().contains(priceMaterial, offer.price())) {
            return PurchaseResult.NOT_ENOUGH;
        }
        int charged = offer.price();
        if (arena != null) {
            ShopPurchaseEvent.Kind kind = switch (onBuyEvent) {
                case GIVE_ARMOR -> ShopPurchaseEvent.Kind.ARMOR;
                case GIVE_TOOL -> ShopPurchaseEvent.Kind.TOOL;
                default -> ShopPurchaseEvent.Kind.ITEM;
            };
            charged = ApiEvents.shopPurchase(arena, player, name, kind, offer.priceLabel(), offer.price(), team);
            if (charged < 0) return PurchaseResult.CANCELLED;
            if (priceMaterial != null && charged > 0 && !player.getInventory().contains(priceMaterial, charged)) {
                return PurchaseResult.NOT_ENOUGH;
            }
        }
        if (priceMaterial != null && charged > 0) {
            player.getInventory().removeItem(new ItemStack(priceMaterial, charged));
        }
        switch (onBuyEvent) {
            case GIVE_ITEM -> {
                ArmorContents.giveOrDrop(player, getItem(player, offer.amount()));
                if (isPermanent && team != null) {
                    team.getArmorContents(player).addOther(this);
                }
                return PurchaseResult.BOUGHT;
            }
            case GIVE_ARMOR -> {
                ArmorContents armorContents = team.getArmorContents(player);
                armorContents.setArmor(ArmorTier.getArmorTier(material));
                armorContents.applyArmor(player);
                return PurchaseResult.BOUGHT;
            }
            case GIVE_TOOL -> {
                ArmorContents armorContents = team.getArmorContents(player);
                boolean isSword = material.name().endsWith("_SWORD");
                if (isSword && armorContents.hasBoughtSword()) {
                    armorContents.setTool(this);
                    ArmorContents.giveOrDrop(player, getItem(player));
                } else {
                    armorContents.setTool(this);
                    armorContents.giveTool(player, this);
                }
                if (isSword) {
                    team.getTeamUpgrades().applyPlayerUpgrades(player);
                }
                return PurchaseResult.BOUGHT;
            }
            case COMMANDS -> {
                runCommands(player, arena, team);
                return PurchaseResult.BOUGHT;
            }
            case OPEN_GUI -> {
                player.openInventory(new com.l299l.newbedwars.gui.configuration.game.guis.ShopGUI(guiManager, player,
                        guiManager.getGui(openGui), null).getInventory());
                return PurchaseResult.BOUGHT;
            }
            case GIVE_PERM -> {
                GamePermissions.grant(NewBedwars.plugin, player, arena.getArenaName(), givePermission);
                return PurchaseResult.BOUGHT;
            }
            default -> {
                return PurchaseResult.UNAVAILABLE;
            }
        }
    }

    private void runCommands(Player player, IArena arena, Team team) {
        for (String line : commands) {
            String command = line.trim();
            boolean asPlayer = command.regionMatches(true, 0, "player:", 0, 7);
            if (asPlayer) command = command.substring(7).trim();
            if (command.startsWith("/")) command = command.substring(1);
            if (!asPlayer && command.contains("{player}") && !SAFE_NAME.matcher(player.getName()).matches()) {
                NewBedwars.plugin.getLogger().warning("Shop item " + name + " did not run '" + command
                        + "' as the console because the player name " + player.getName() + " is not a normal Minecraft name");
                continue;
            }
            command = command.replace("{player}", player.getName())
                    .replace("{team}", team == null ? "" : team.getName())
                    .replace("{arena}", arena == null ? "" : arena.getArenaName());
            if (command.isEmpty()) continue;
            try {
                if (asPlayer) player.performCommand(command);
                else org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
            } catch (RuntimeException e) {
                NewBedwars.plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Shop item " + name + " could not run the command '" + command + "'", e);
            }
        }
    }

    public String priceLabel() {
        if (priceType == PriceType.OTHER) return otherPriceMaterial == null ? PriceType.OTHER.name() : otherPriceMaterial.name();
        return priceType.name();
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
    }

    public String getOpenGui() {
        return openGui;
    }

    public void setOpenGui(String openGui) {
        this.openGui = openGui == null ? "" : openGui;
    }

    public String getGivePermission() {
        return givePermission;
    }

    public void setGivePermission(String givePermission) {
        this.givePermission = givePermission == null ? "" : givePermission;
    }

    public Material getOtherPriceMaterial() {
        return otherPriceMaterial;
    }

    public void setOtherPriceMaterial(Material otherPriceMaterial) {
        this.otherPriceMaterial = otherPriceMaterial;
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
        return buildIcon(language, material, null, ItemPricing.base(this));
    }

    public ItemStack getIcon(Language language, Player viewer) {
        return buildIcon(language, material, viewer, ItemPricing.base(this));
    }

    @Override
    public ItemStack getIcon(Player player) {
        return getIcon(player, ItemPricing.offer(this, null, ArenaRegistry.byWorld(player.getWorld())));
    }

    public ItemStack getIcon(Player player, ItemPricing.Offer offer) {
        Language language = NewBedwars.plugin.getPlayerManager().getPlayer(player.getName()).language();
        Material iconMaterial = material;
        if (TeamColor.undyedKind(material) != null) {
            IArena arena = ArenaRegistry.byWorld(player.getWorld());
            if (arena != null) {
                Team team = arena.getTeam(player);
                if (team != null) iconMaterial = TeamColor.recolor(material, team.getColor());
            }
        }
        return buildIcon(language, iconMaterial, player, offer);
    }

    private ItemStack buildIcon(Language language, Material iconMaterial, Player viewer, ItemPricing.Offer offer) {
        String desc = msg.getMsg(language, name + "-icon-description");
        if (desc == null || desc.isEmpty()) {
            desc = msg.getMsg(language, iconDesc);
        }
        if (desc == null) desc = "";
        if (onBuyEvent == OnBuyEvent.UPGRADE) {

            desc = Arrays.stream(desc.split(";"))
                    .filter(line -> !line.contains("/price/"))
                    .collect(Collectors.joining(";"));
        }
        desc = desc.replace("/price/", offer.price() + " " + offer.priceLabel());
        desc = desc.replace("/amount/", String.valueOf(offer.amount()));
        String displayName = msg.getMsg(language, name + "-name");
        if (displayName == null) displayName = name;
        desc = desc.replace("/name/", displayName);
        displayName = com.l299l.newbedwars.placeholder.PapiText.apply(viewer, displayName);
        desc = com.l299l.newbedwars.placeholder.PapiText.apply(viewer, desc);
        ItemStack icon = guiManager.createIcon(iconMaterial.name(), displayName, desc, (enchantments != null && !enchantments.isEmpty()));
        if (viewer == null) return icon;
        return ApiEvents.shopItemStack(viewer, name, com.l299l.newbedwars.api.events.ShopItemStackEvent.Use.ICON, icon);
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

    public Map<String, Object> getAsHashMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", itemDesc);
        map.put("material", material.name());
        map.put("price", price);
        map.put("priceType", priceType.name());
        map.put("amount", amount);
        map.put("iconDesc", iconDesc);
        map.put("permission", permission);
        map.put("itemLogic", event.getKey());
        map.put("onBuyEvent", onBuyEvent.name());
        map.put("isPermanent", isPermanent);
        map.put("enchantments", getEnchantmentsAsHashMap());
        map.put("itemFlags", itemFlagNames());
        if (!commands.isEmpty()) map.put("commands", new ArrayList<>(commands));
        if (!openGui.isEmpty()) map.put("gui", openGui);
        if (!givePermission.isEmpty()) map.put("givePermission", givePermission);
        if (otherPriceMaterial != null) map.put("priceMaterial", otherPriceMaterial.name());
        return map;
    }


    private Map<String, Integer> getEnchantmentsAsHashMap() {
        Map<String, Integer> map = new java.util.TreeMap<>();
        if (enchantments == null) return map;
        for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
            NamespacedKey key = e.getKey().getKey();
            map.put(key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.toString(), e.getValue());
        }
        return map;
    }

    private List<String> itemFlagNames() {
        List<String> list = new ArrayList<>();
        if (itemFlags == null) return list;
        for (ItemFlag itemFlag : itemFlags) {
            list.add(itemFlag.name());
        }
        return list;
    }
}
