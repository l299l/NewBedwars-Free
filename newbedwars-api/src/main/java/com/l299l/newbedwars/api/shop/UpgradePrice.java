package com.l299l.newbedwars.api.shop;

import java.util.List;

/**
 * The server-wide prices of one upgrade or trap from {@code data/upgrades.yml}.
 *
 * @param name     upgrade name as used in {@code upg:<name>}
 * @param tiers    price of level 1, 2, ... in order; the number of tiers is the number of buyable levels
 * @param maxLevel the most levels this upgrade supports; tiers above it are ignored
 * @param builtIn  false for upgrades registered by addons
 */
public record UpgradePrice(String name, List<UpgradeTier> tiers, int maxLevel, boolean builtIn) {
    public UpgradePrice {
        tiers = tiers == null ? List.of() : List.copyOf(tiers);
    }
}
