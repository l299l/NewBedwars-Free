package com.l299l.newbedwars.api.shop;

/**
 * Which GUIs an arena's shops open.
 *
 * @param arena      arena name
 * @param waiting    true while the arena is waiting, the only state in which the GUIs can be changed
 * @param shopGui    id of the item shop GUI
 * @param upgradeGui id of the team upgrade GUI
 */
public record ArenaShops(String arena, boolean waiting, String shopGui, String upgradeGui) {}
