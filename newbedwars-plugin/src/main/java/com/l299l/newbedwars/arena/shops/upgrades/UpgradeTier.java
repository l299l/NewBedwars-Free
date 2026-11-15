package com.l299l.newbedwars.arena.shops.upgrades;

import com.l299l.newbedwars.arena.shops.customitems.PriceType;
import org.bukkit.Material;

import java.util.Locale;

public record UpgradeTier(int price, PriceType currency) {

    public UpgradeTier {
        if (price < 0) throw new IllegalArgumentException("price must not be negative");
        if (!isCurrency(currency)) throw new IllegalArgumentException("unknown currency " + currency);
    }

    public Material material() {
        return switch (currency) {
            case IRON -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case EMERALD -> Material.EMERALD;
            default -> Material.DIAMOND;
        };
    }

    public static boolean isCurrency(PriceType type) {
        return type == PriceType.IRON || type == PriceType.GOLD || type == PriceType.DIAMOND || type == PriceType.EMERALD;
    }

    public static PriceType parseCurrency(Object value, PriceType fallback) {
        if (value == null) return fallback;
        try {
            PriceType type = PriceType.valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
            if (isCurrency(type)) return type;
        } catch (IllegalArgumentException ignored) {
        }
        throw new IllegalArgumentException("unknown currency " + value + " (IRON, GOLD, DIAMOND or EMERALD)");
    }
}
