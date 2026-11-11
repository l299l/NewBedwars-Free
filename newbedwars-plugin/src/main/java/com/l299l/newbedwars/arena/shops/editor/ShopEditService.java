package com.l299l.newbedwars.arena.shops.editor;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.shop.ItemTexts;
import com.l299l.newbedwars.api.shop.ShopCategory;
import com.l299l.newbedwars.api.shop.ShopGui;
import com.l299l.newbedwars.api.shop.ShopItem;
import com.l299l.newbedwars.api.shop.ShopOptions;
import com.l299l.newbedwars.api.shop.ShopResult;
import com.l299l.newbedwars.api.shop.ShopResult.Code;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.shops.customitems.CustomItem;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.CustomLogicRegistry;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.LogicType;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.OnBuyEvent;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.data.yaml.guis.GUIDataManager;
import com.l299l.newbedwars.config.data.yaml.guis.GuiDefinition;
import com.l299l.newbedwars.config.data.yaml.guis.GuiRefs;
import com.l299l.newbedwars.config.data.yaml.items.ItemsDataManager;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUISave;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class ShopEditService {
    static final Pattern ITEM_NAME = Pattern.compile("[A-Za-z0-9_]{1,32}");
    static final Pattern GUI_ID = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    static final List<String> OFFERED_ON_BUY = List.of("GIVE_ITEM", "GIVE_ARMOR", "GIVE_TOOL", "UPGRADE", "CANCEL");
    private static final String TEXTS = "CustomItemsNames.";
    private static final String DEFAULT_LORE_KEY = "shop-item-description";

    private ShopEditService() {}

    private static CustomItemManager items() {
        return NewBedwars.plugin.getCustomItemManager();
    }

    private static ItemsDataManager itemsData() {
        return NewBedwars.plugin.getDataManager().getItemsDataManager();
    }

    private static GUIDataManager guiData() {
        return NewBedwars.plugin.getDataManager().getGuiDataManager();
    }

    public static List<ShopItem> listItems() {
        List<ShopItem> out = new ArrayList<>();
        for (CustomItem item : items().getCustomItems()) out.add(toApi(item));
        return out;
    }

    public static Optional<ShopItem> item(String name) {
        CustomItem item = name == null ? null : items().getCustomItem(name);
        return item == null ? Optional.empty() : Optional.of(toApi(item));
    }

    static ShopItem toApi(CustomItem item) {
        Map<String, Integer> enchantments = new TreeMap<>();
        if (item.getEnchantments() != null) {
            item.getEnchantments().forEach((e, level) -> enchantments.put(enchantmentKey(e), level));
        }
        List<String> flags = new ArrayList<>();
        if (item.getItemFlags() != null) for (ItemFlag f : item.getItemFlags()) flags.add(f.name());
        return new ShopItem(item.getName(), item.getMaterial().name(), item.getAmount(), item.getPrice(),
                item.getPriceType().name(), item.getOnBuyEvent().name(), item.getEvent().getKey(), item.isPermanent(),
                item.getPermission() == null || item.getPermission().equalsIgnoreCase("default") ? "" : item.getPermission(),
                enchantments, flags, itemUsages(item.getName()));
    }

    static String enchantmentKey(Enchantment e) {
        NamespacedKey key = e.getKey();
        return key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.toString();
    }

    public static List<String> itemUsages(String name) {
        Set<String> out = new java.util.LinkedHashSet<>();
        for (GUISave save : guiData().getGuiSaves()) {
            GuiDefinition d = save.getDefinition();
            if (d != null && usesItem(d, name)) out.add("gui:" + d.id());
        }
        if (Properties.BasicSword != null && Properties.BasicSword.equalsIgnoreCase(name)) out.add("config:BasicSword");
        return new ArrayList<>(out);
    }

    static boolean usesItem(GuiDefinition d, String name) {
        for (String token : d.slots()) if (GuiRefs.refersToItem(token, name, false)) return true;
        for (String token : List.of(d.fill(), d.empty())) {
            GuiRefs.Ref ref = GuiRefs.fillOrEmpty(token);
            if (ref.kind() == GuiRefs.Kind.ITEM && ref.target().equalsIgnoreCase(name)) return true;
        }
        for (GuiDefinition.Category c : d.categories()) {
            if (c.icon().equalsIgnoreCase(name)) return true;
            for (String token : c.items()) if (GuiRefs.refersToItem(token, name, true)) return true;
        }
        return false;
    }

    public static ShopResult saveItem(ShopItem spec) {
        if (spec == null || spec.name() == null || !ITEM_NAME.matcher(spec.name()).matches()) {
            return invalid("name: use 1 to 32 letters, digits or _");
        }
        CustomItem existing = items().getCustomItem(spec.name());
        if (existing == null && itemsData().isBroken(spec.name())) {
            return new ShopResult(Code.NAME_TAKEN, "items.yml has an item with this name that can not be loaded on this server");
        }
        Material material = Material.matchMaterial(spec.material() == null ? "" : spec.material());
        if (material == null || !material.isItem() || material.isAir()) return invalid("material: unknown item " + spec.material());
        if (spec.amount() < 1 || spec.amount() > 64) return invalid("amount: 1 to 64");
        if (spec.price() < -1 || spec.price() > 100000) return invalid("price: 0 to 100000");
        PriceType priceType = parseEnum(PriceType.class, spec.priceType());
        if (priceType == null) return invalid("priceType: unknown " + spec.priceType());
        OnBuyEvent onBuy = parseEnum(OnBuyEvent.class, spec.onBuy());
        if (onBuy == null) return invalid("onBuy: unknown " + spec.onBuy());
        String logic = spec.logic() == null || spec.logic().isBlank() ? "NONE" : CustomLogicRegistry.normalize(spec.logic());
        boolean logicKnown = CustomLogicRegistry.isBuiltIn(logic) || CustomLogicRegistry.keys().contains(logic)
                || (existing != null && existing.getEvent().getKey().equalsIgnoreCase(logic));
        if (!logicKnown) return invalid("logic: unknown " + spec.logic());
        String permission = spec.permission().trim();
        if (permission.length() > 128 || permission.contains(" ")) return invalid("permission: up to 128 characters, no spaces");
        HashMap<Enchantment, Integer> enchantments = new HashMap<>();
        for (Map.Entry<String, Integer> e : spec.enchantments().entrySet()) {
            Enchantment enchantment = enchantment(e.getKey());
            if (enchantment == null) return invalid("enchantments: unknown " + e.getKey());
            int level = e.getValue() == null ? 0 : e.getValue();
            if (level < 1 || level > 255) return invalid("enchantments: level of " + e.getKey() + " must be 1 to 255");
            enchantments.put(enchantment, level);
        }
        List<ItemFlag> flags = new ArrayList<>();
        for (String f : spec.flags()) {
            ItemFlag flag = parseEnum(ItemFlag.class, f);
            if (flag == null) return invalid("flags: unknown " + f);
            if (!flags.contains(flag)) flags.add(flag);
        }

        CustomItem item = existing;
        if (item == null) {
            items().createCustomItem(spec.name(), "default", material, spec.price(), priceType, spec.amount(), "default",
                    permission, logic, onBuy, spec.permanent(), enchantments, flags, true);
            item = items().getCustomItem(spec.name());
            NewBedwars.plugin.getLangMessages().addCustomItemProperty(spec.name(), "-name");
            if (readText(Language.English, spec.name() + "-name").isEmpty()) {
                writeTexts(Map.of(Language.English, Map.of(TEXTS + spec.name() + "-name", spec.name())));
            }
        } else {
            item.setMaterial(material);
            item.setAmount(spec.amount());
            item.setPrice(spec.price());
            item.setPriceType(priceType);
            item.setOnBuyEvent(onBuy);
            if (!item.getEvent().getKey().equalsIgnoreCase(logic)) item.setEvent(items().matchLogicKey(logic));
            item.setPermanent(spec.permanent());
            item.setPermission(permission);
            item.setEnchantments(enchantments);
            item.setItemFlags(flags);
        }
        itemsData().save();
        refreshGuis();
        return ok();
    }

    public static ShopResult deleteItem(String name) {
        if (name == null) return new ShopResult(Code.NOT_FOUND, null);
        boolean broken = itemsData().isBroken(name);
        if (items().getCustomItem(name) == null && !broken) return new ShopResult(Code.NOT_FOUND, null);
        List<String> usages = itemUsages(name);
        if (!usages.isEmpty()) return new ShopResult(Code.IN_USE, String.join(", ", usages));
        items().remove(name);
        itemsData().remove(name);
        itemsData().save();
        refreshGuis();
        return ok();
    }

    public static Optional<Map<String, ItemTexts>> texts(String name) {
        CustomItem item = name == null ? null : items().getCustomItem(name);
        if (item == null) return Optional.empty();
        Map<String, ItemTexts> out = new LinkedHashMap<>();
        String loreKey = loreKey(item);
        for (Language lang : Language.values()) {
            String lore = readText(lang, loreKey);
            if (lore.isEmpty() && !item.getDescription().equals(DEFAULT_LORE_KEY)) lore = readTopLevel(lang, item.getDescription());
            out.put(lang.getCode(), new ItemTexts(readText(lang, item.getName() + "-name"),
                    readText(lang, item.getName() + "-icon-description"), lore));
        }
        return Optional.of(out);
    }

    public static ShopResult saveTexts(String name, Map<String, ItemTexts> texts) {
        CustomItem item = name == null ? null : items().getCustomItem(name);
        if (item == null) return new ShopResult(Code.NOT_FOUND, null);
        if (texts == null || texts.isEmpty()) return invalid("texts: nothing to save");
        Map<Language, Map<String, String>> changes = new LinkedHashMap<>();
        for (Map.Entry<String, ItemTexts> e : texts.entrySet()) {
            Language lang = Language.fromCode(e.getKey());
            if (lang == null) return invalid("language: unknown " + e.getKey());
            ItemTexts t = e.getValue() == null ? new ItemTexts("", "", "") : e.getValue();
            if (t.name().length() > 128 || t.iconDescription().length() > 2048 || t.lore().length() > 2048) {
                return invalid("texts: name up to 128, descriptions up to 2048 characters");
            }
            String displayName = t.name().isEmpty() && lang == Language.English ? item.getName() : t.name();
            Map<String, String> values = new LinkedHashMap<>();
            values.put(TEXTS + item.getName() + "-name", displayName);
            values.put(TEXTS + item.getName() + "-icon-description", t.iconDescription());
            values.put(TEXTS + item.getName() + "-description", t.lore());
            changes.put(lang, values);
        }
        writeTexts(changes);
        boolean anyLore = false;
        for (Language lang : Language.values()) {
            if (!readText(lang, item.getName() + "-description").isEmpty()) anyLore = true;
        }
        String wanted = anyLore ? item.getName() + "-description" : DEFAULT_LORE_KEY;
        if (!wanted.equals(item.getDescription())) {
            item.setDescription(wanted);
            itemsData().save();
        }
        return ok();
    }

    private static String loreKey(CustomItem item) {
        return item.getDescription().equals(DEFAULT_LORE_KEY) ? item.getName() + "-description" : item.getDescription();
    }

    private static org.bukkit.configuration.file.FileConfiguration langConf(Language lang) {
        return NewBedwars.plugin.getMessages().langConf(lang);
    }

    private static String readText(Language lang, String key) {
        return readTopLevel(lang, key.startsWith(TEXTS) ? key : TEXTS + key);
    }

    private static String readTopLevel(Language lang, String path) {
        org.bukkit.configuration.file.FileConfiguration conf = langConf(lang);
        String value = conf == null ? null : conf.getString(path);
        return value == null ? "" : value;
    }

    private static void writeTexts(Map<Language, Map<String, String>> changes) {
        for (Map.Entry<Language, Map<String, String>> e : changes.entrySet()) {
            Language lang = e.getKey();
            org.bukkit.configuration.file.FileConfiguration conf = langConf(lang);
            if (conf == null) continue;
            for (Map.Entry<String, String> v : e.getValue().entrySet()) {
                String value = v.getValue();
                conf.set(v.getKey(), value == null || value.isEmpty() ? null : value);
                NewBedwars.plugin.getLangMessages().setText(lang, v.getKey().substring(TEXTS.length()), value);
            }
            NewBedwars.plugin.getMessages().saveLangFile(lang, true);
        }
    }

    public static List<ShopGui> listGuis() {
        List<ShopGui> out = new ArrayList<>();
        for (GUISave save : guiData().getGuiSaves()) out.add(toApi(save));
        return out;
    }

    public static Optional<ShopGui> gui(String id) {
        GUISave save = id == null ? null : guiData().get(id);
        return save == null ? Optional.empty() : Optional.of(toApi(save));
    }

    static ShopGui toApi(GUISave save) {
        GuiDefinition d = save.getDefinition();
        List<ShopCategory> categories = new ArrayList<>();
        for (GuiDefinition.Category c : d.categories()) {
            categories.add(new ShopCategory(c.id(), c.name(), c.icon(), c.description(), c.items()));
        }
        return new ShopGui(d.id(), d.title(), d.size(), d.headerSize(), d.footerSize(), d.closeOnTransaction(),
                d.fill(), d.empty(), d.slots(), categories, save.getProblems(), guiUsages(d.id()));
    }

    public static List<String> guiUsages(String id) {
        List<String> out = new ArrayList<>();
        for (IArena arena : ArenaRegistry.all()) {
            if (id.equals(arena.getShopGuiId()) || id.equals(arena.getUpgradeGuiId())) out.add("arena:" + arena.getArenaName());
        }
        if (id.equals(Properties.DefaultTeamShopGui)) out.add("config:DefaultTeamShopGui");
        if (id.equals(Properties.DefaultUpgradeShopGui)) out.add("config:DefaultTeamUpgradeGui");
        return out;
    }

    public static ShopResult createGui(ShopGui gui) {
        if (gui == null || gui.id() == null || !GUI_ID.matcher(gui.id()).matches()) {
            return invalid("id: use 1 to 32 letters, digits, _ or -");
        }
        if (guiData().get(gui.id()) != null || guiData().fileTaken(gui.id())) return new ShopResult(Code.NAME_TAKEN, null);
        for (GUISave save : guiData().getGuiSaves()) {
            if (save.getGuiId().equalsIgnoreCase(gui.id())) return new ShopResult(Code.NAME_TAKEN, null);
        }
        return store(gui, null);
    }

    public static ShopResult saveGui(ShopGui gui) {
        GUISave existing = gui == null || gui.id() == null ? null : guiData().get(gui.id());
        if (existing == null) return new ShopResult(Code.NOT_FOUND, null);
        return store(gui, existing.getDefinition());
    }

    private static ShopResult store(ShopGui gui, GuiDefinition previous) {
        String problem = validate(gui, previous);
        if (problem != null) return invalid(problem);
        List<GuiDefinition.Category> categories = new ArrayList<>();
        for (ShopCategory c : gui.categories()) {
            categories.add(new GuiDefinition.Category(c.id(), c.name(), c.icon(), c.description(), c.items()));
        }
        GuiDefinition def = new GuiDefinition(gui.id(), previous == null ? "ShopGUI" : previous.guiClass(), gui.title(),
                gui.size(), gui.closeOnTransaction(), blankToEmpty(gui.fill()), blankToEmpty(gui.empty()),
                gui.headerSize(), gui.footerSize(), gui.slots(), categories, previous == null ? Map.of() : previous.extra());
        guiData().save(def);
        NewBedwars.plugin.getGuiManager().refresh();
        return ok();
    }

    public static ShopResult deleteGui(String id) {
        if (id == null || guiData().get(id) == null) return new ShopResult(Code.NOT_FOUND, null);
        List<String> usages = guiUsages(id);
        if (!usages.isEmpty()) return new ShopResult(Code.IN_USE, String.join(", ", usages));
        guiData().delete(id);
        NewBedwars.plugin.getGuiManager().refresh();
        return ok();
    }

    static String validate(ShopGui gui, GuiDefinition previous) {
        if (gui.title() == null || gui.title().isBlank() || gui.title().length() > 64) return "title: 1 to 64 characters";
        int size = gui.size();
        if (size < 9 || size > 54 || size % 9 != 0) return "size: 9, 18, 27, 36, 45 or 54";
        if (gui.slots().size() != size) return "slots: " + size + " entries needed";
        if (gui.headerSize() < 0 || gui.headerSize() % 9 != 0 || gui.footerSize() < 0 || gui.footerSize() % 9 != 0
                || gui.headerSize() + gui.footerSize() > size) {
            return "headerSize/footerSize: multiples of 9 that fit in the size";
        }
        Set<String> allowed = new HashSet<>();
        if (previous != null) {
            allowed.addAll(previous.slots());
            allowed.add(previous.fill());
            allowed.add(previous.empty());
            for (GuiDefinition.Category c : previous.categories()) {
                allowed.add("icon:" + c.icon());
                allowed.addAll(c.items());
            }
        }
        Set<String> categoryIds = new HashSet<>();
        if (gui.categories().size() > 54) return "categories: at most 54";
        for (ShopCategory c : gui.categories()) {
            if (c.id() == null || !GUI_ID.matcher(c.id()).matches()) return "categories: id " + c.id() + " may only use letters, digits, _ or -";
            if (!categoryIds.add(c.id())) return "categories: id " + c.id() + " is used twice";
            if (c.name() == null || c.name().isBlank() || c.name().length() > 64) return "category " + c.id() + ": name 1 to 64 characters";
            if (c.description().length() > 256) return "category " + c.id() + ": description up to 256 characters";
            if (c.icon() == null || (items().getCustomItem(c.icon()) == null && !allowed.contains("icon:" + c.icon()))) {
                return "category " + c.id() + ": unknown icon item " + c.icon();
            }
            if (c.items().size() > 54) return "category " + c.id() + ": at most 54 items";
            for (String token : c.items()) {
                if (allowed.contains(token)) continue;
                String p = checkRef(GuiRefs.categoryItem(token), token, null);
                if (p != null) return "category " + c.id() + ": " + p;
            }
        }
        for (String token : List.of(blankToEmpty(gui.fill()), blankToEmpty(gui.empty()))) {
            if (allowed.contains(token)) continue;
            GuiRefs.Ref ref = GuiRefs.fillOrEmpty(token);
            if (ref.kind() == GuiRefs.Kind.EMPTY) continue;
            String p = checkRef(ref, token, null);
            if (p != null) return "fill/empty: " + p;
        }
        for (int i = 0; i < gui.slots().size(); i++) {
            String token = gui.slots().get(i);
            if (token == null) return "slot " + (i + 1) + ": missing";
            if (allowed.contains(token)) continue;
            String p = checkRef(GuiRefs.slot(token), token, categoryIds);
            if (p != null) return "slot " + (i + 1) + ": " + p;
        }
        return null;
    }

    private static String checkRef(GuiRefs.Ref ref, String token, Set<String> categoryIds) {
        return switch (ref.kind()) {
            case FILL, EMPTY -> null;
            case ITEM -> items().getCustomItem(ref.target()) == null ? "unknown item " + ref.target() : null;
            case UPGRADE -> {
                if (!upgradeExists(ref.target())) yield "unknown upgrade " + ref.target();
                yield items().getCustomItem(ref.target()) == null ? "no icon item named " + ref.target() : null;
            }
            case CATEGORY -> categoryIds != null && categoryIds.contains(ref.target()) ? null : "unknown category " + ref.target();
            case INVALID -> "invalid value " + token;
        };
    }

    private static boolean upgradeExists(String name) {
        try {
            Upgrade.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static List<com.l299l.newbedwars.api.shop.ArenaShops> arenaShops() {
        List<com.l299l.newbedwars.api.shop.ArenaShops> out = new ArrayList<>();
        for (IArena arena : ArenaRegistry.all()) {
            out.add(new com.l299l.newbedwars.api.shop.ArenaShops(arena.getArenaName(),
                    arena.status() == com.l299l.newbedwars.arena.GameStatus.waiting, arena.getShopGuiId(), arena.getUpgradeGuiId()));
        }
        out.sort(java.util.Comparator.comparing(com.l299l.newbedwars.api.shop.ArenaShops::arena, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static ShopResult setArenaShops(String arenaName, String shopGui, String upgradeGui) {
        IArena arena = arenaName == null ? null : ArenaRegistry.find(arenaName);
        if (arena == null) return new ShopResult(Code.NOT_FOUND, null);
        if (shopGui != null && guiData().get(shopGui) == null) return invalid("shopGui: unknown GUI " + shopGui);
        if (upgradeGui != null && guiData().get(upgradeGui) == null) return invalid("upgradeGui: unknown GUI " + upgradeGui);
        if (arena.status() != com.l299l.newbedwars.arena.GameStatus.waiting) return new ShopResult(Code.BUSY, null);
        boolean changed = false;
        if (shopGui != null && !shopGui.equals(arena.getShopGuiId())) {
            arena.setShopGuiId(shopGui);
            changed = true;
        }
        if (upgradeGui != null && !upgradeGui.equals(arena.getUpgradeGuiId())) {
            arena.setUpgradeGuiId(upgradeGui);
            changed = true;
        }
        if (changed) com.l299l.newbedwars.apiimpl.ApiPersistence.requestSave(arena);
        return ok();
    }

    public static ShopOptions options() {
        List<String> materials = Arrays.stream(Material.values())
                .filter(m -> !isLegacy(m) && m.isItem() && !m.isAir()).map(Enum::name).sorted().toList();
        List<String> enchantments = new ArrayList<>();
        for (Enchantment e : Registry.ENCHANTMENT) enchantments.add(enchantmentKey(e));
        enchantments.sort(null);
        List<String> flags = Arrays.stream(ItemFlag.values()).map(Enum::name).toList();
        List<String> logic = new ArrayList<>();
        for (LogicType t : LogicType.values()) logic.add(t.name());
        logic.addAll(CustomLogicRegistry.keys());
        List<String> upgrades = Upgrade.values().stream().map(Upgrade::name).toList();
        List<String> priceTypes = Arrays.stream(PriceType.values()).map(Enum::name).toList();
        List<String> languages = Arrays.stream(Language.values()).map(Language::getCode).toList();
        return new ShopOptions(materials, enchantments, flags, logic, upgrades, priceTypes, OFFERED_ON_BUY, languages);
    }

    @SuppressWarnings("deprecation")
    private static boolean isLegacy(Material m) {
        return m.isLegacy();
    }

    @SuppressWarnings("deprecation")
    private static Enchantment enchantment(String key) {
        if (key == null) return null;
        NamespacedKey nk = NamespacedKey.fromString(key.trim().toLowerCase(Locale.ROOT));
        return nk == null ? null : Enchantment.getByKey(nk);
    }

    private static void refreshGuis() {
        guiData().rebuild();
        NewBedwars.plugin.getGuiManager().refresh();
    }

    private static String blankToEmpty(String s) {
        return s == null || s.isBlank() ? "empty" : s.trim();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ShopResult invalid(String detail) {
        return new ShopResult(Code.INVALID, detail);
    }

    private static ShopResult ok() {
        return new ShopResult(Code.OK, null);
    }
}
