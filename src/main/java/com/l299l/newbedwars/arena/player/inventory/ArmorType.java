package com.l299l.newbedwars.arena.player.inventory;

import org.bukkit.Material;

public enum ArmorType {
    BASIC, GOLD, CHAINMAIL, IRON, DIAMOND, NETHERITE, OTHER;

    public static ArmorType getArmorType(Material material) {
        switch (material) {
            case LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS -> {
                return BASIC;
            }
            case GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS -> {
                return GOLD;
            }
            case CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_LEGGINGS, CHAINMAIL_BOOTS -> {
                return CHAINMAIL;
            }
            case IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> {
                return IRON;
            }
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> {
                return DIAMOND;
            }
            case NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> {
                return NETHERITE;
            }
            default -> {
                return OTHER;
            }
        }
    }
}
