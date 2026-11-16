package com.l299l.newbedwars.api.shop;

/**
 * Which GUIs an arena's shops open.
 *
 * @param arena      arena name
 * @param waiting    whether the arena is waiting. The GUIs can only be changed then
 * @param shopGui    id of the item shop GUI
 * @param upgradeGui id of the team upgrade GUI
 * @since 2.7-beta
 */
public record ArenaShops(String arena, boolean waiting, String shopGui, String upgradeGui) {}
