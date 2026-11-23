package com.l299l.newbedwars.api.shop;

/**
 * A per-GUI override of one shop item's price and amount ({@code ItemPrices:} in a GUI file).
 *
 * @param price    amount of the currency, 0 to 100000
 * @param currency {@code IRON}, {@code GOLD}, {@code DIAMOND} or {@code EMERALD}; {@code null} keeps the item's own currency
 * @param amount   how many the player gets, 1 to 64; 0 keeps the item's own amount
 * @since 2.8-beta
 */
public record ItemPrice(int price, String currency, int amount) {
}
