package com.l299l.newbedwars.arena.shops.customitems;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.shop.ItemPrice;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.upgrades.UpgradeTier;
import com.l299l.newbedwars.config.data.yaml.guis.GuiDefinition;
import com.l299l.newbedwars.gui.GUISave;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class ItemPricing {
    public static final String GUI_KEY = "ItemPrices";
    public static final int MAX_PRICE = 100000;
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    public record Offer(int price, PriceType priceType, Material otherMaterial, int amount) {
        public Material priceMaterial() {
            return switch (priceType) {
                case EMERALD -> Material.EMERALD;
                case DIAMOND -> Material.DIAMOND;
                case GOLD -> Material.GOLD_INGOT;
                case IRON -> Material.IRON_INGOT;
                case OTHER -> otherMaterial;
                default -> null;
            };
        }

        public String priceLabel() {
            if (priceType == PriceType.OTHER) return otherMaterial == null ? PriceType.OTHER.name() : otherMaterial.name();
            return priceType.name();
        }
    }

    private ItemPricing() {}

    public static Offer base(CustomItem item) {
        return new Offer(item.getPrice(), item.getPriceType(), item.getOtherPriceMaterial(), item.getAmount());
    }

    public static Offer offer(CustomItem item, GuiDefinition shown, IArena arena) {
        Offer base = base(item);
        Offer own = override(item, base, shown);
        if (own != null) return own;
        GuiDefinition shop = arenaShop(arena);
        if (shop != null && (shown == null || !shop.id().equals(shown.id()))) {
            Offer fromShop = override(item, base, shop);
            if (fromShop != null) return fromShop;
        }
        return base;
    }

    public static Offer apply(Offer base, ItemPrice price) {
        PriceType type = base.priceType();
        Material other = base.otherMaterial();
        if (price.currency() != null) {
            type = UpgradeTier.parseCurrency(price.currency(), null);
            other = null;
        }
        int amount = price.amount() > 0 ? price.amount() : base.amount();
        return new Offer(price.price(), type, other, amount);
    }

    public static Map<String, ItemPrice> overrides(GuiDefinition gui) {
        Map<String, ItemPrice> out = new LinkedHashMap<>();
        if (gui == null || !(gui.extra().get(GUI_KEY) instanceof Map<?, ?> map)) return out;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            try {
                out.put(String.valueOf(e.getKey()), parse(e.getValue()));
            } catch (IllegalArgumentException ex) {
                warn(gui, String.valueOf(e.getKey()), e.getValue(), ex.getMessage());
            }
        }
        return out;
    }

    public static ItemPrice parse(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) throw new IllegalArgumentException("expected {price: <n>, currency: <c>, amount: <n>}");
        for (Object key : m.keySet()) {
            String k = String.valueOf(key);
            if (!k.equals("price") && !k.equals("currency") && !k.equals("amount")) {
                throw new IllegalArgumentException("unknown field " + k + " (price, currency, amount)");
            }
        }
        if (!m.containsKey("price")) throw new IllegalArgumentException("price is missing");
        int price = whole(m.get("price"), 0, MAX_PRICE, "price");
        String currency = null;
        if (m.get("currency") != null) {
            currency = UpgradeTier.parseCurrency(m.get("currency"), null).name();
        }
        int amount = m.get("amount") == null ? 0 : whole(m.get("amount"), 1, 64, "amount");
        return new ItemPrice(price, currency, amount);
    }

    public static ItemPrice validate(ItemPrice price) {
        if (price == null) throw new IllegalArgumentException("missing value");
        if (price.price() < 0 || price.price() > MAX_PRICE) throw new IllegalArgumentException("price: 0 to " + MAX_PRICE);
        if (price.amount() < 0 || price.amount() > 64) throw new IllegalArgumentException("amount: 1 to 64, or 0 to keep the item's amount");
        String currency = price.currency() == null || price.currency().isBlank() ? null
                : UpgradeTier.parseCurrency(price.currency(), null).name();
        return new ItemPrice(price.price(), currency, price.amount());
    }

    public static Map<String, Object> serialize(ItemPrice price) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("price", price.price());
        if (price.currency() != null) m.put("currency", price.currency().toUpperCase(Locale.ROOT));
        if (price.amount() > 0) m.put("amount", price.amount());
        return m;
    }

    private static Offer override(CustomItem item, Offer base, GuiDefinition gui) {
        if (gui == null || !(gui.extra().get(GUI_KEY) instanceof Map<?, ?> map)) return null;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (!key.equalsIgnoreCase(item.getName())) continue;
            try {
                return apply(base, parse(e.getValue()));
            } catch (IllegalArgumentException ex) {
                warn(gui, key, e.getValue(), ex.getMessage());
                return null;
            }
        }
        return null;
    }

    private static GuiDefinition arenaShop(IArena arena) {
        if (arena == null || NewBedwars.plugin == null || NewBedwars.plugin.getGuiManager() == null) return null;
        String id = arena.getShopGuiId();
        GUISave save = id == null ? null : NewBedwars.plugin.getGuiManager().getGui(id);
        return save == null ? null : save.getDefinition();
    }

    private static int whole(Object value, int min, int max, String field) {
        if (!(value instanceof Integer || value instanceof Long || value instanceof Short)) {
            throw new IllegalArgumentException(field + " must be a whole number, not '" + value + "'");
        }
        long v = ((Number) value).longValue();
        if (v < min || v > max) throw new IllegalArgumentException(field + ": " + min + " to " + max);
        return (int) v;
    }

    private static void warn(GuiDefinition gui, String item, Object raw, String problem) {
        if (!WARNED.add(gui.id() + "/" + item + "/" + raw)) return;
        Logger log = NewBedwars.plugin == null ? Logger.getLogger("NewBedwars") : NewBedwars.plugin.getLogger();
        log.warning("ItemPrices entry " + item + " in GUI " + gui.id() + " is invalid: " + problem
                + " (the item's own price is used)");
    }
}
