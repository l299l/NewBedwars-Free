package com.l299l.newbedwars.api.shop;

import java.util.List;

/**
 * The server-wide prices of one upgrade or trap.
 *
 * @param name     upgrade name as used in {@code upg:<name>}
 * @param tiers    price of level 1, 2, ... in order; the number of tiers is the number of buyable levels
 * @param maxLevel the most levels this upgrade supports. Extra tiers in the prices file are ignored, and
 *                 {@link ShopEditor#saveUpgradePrices} rejects them
 * @param builtIn  false for upgrades registered by addons
 * @since 2.7-beta
 */
public record UpgradePrice(String name, List<UpgradeTier> tiers, int maxLevel, boolean builtIn) {
    public UpgradePrice {
        tiers = tiers == null ? List.of() : List.copyOf(tiers);
    }
}
