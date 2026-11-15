package com.l299l.newbedwars.api.shop;

/**
 * One buyable level of a team upgrade or trap.
 *
 * @param price    amount of the currency, 0 or more
 * @param currency {@code IRON}, {@code GOLD}, {@code DIAMOND} or {@code EMERALD}
 */
public record UpgradeTier(int price, String currency) {
}
