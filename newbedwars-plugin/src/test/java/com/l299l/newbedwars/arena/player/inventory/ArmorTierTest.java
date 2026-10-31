package com.l299l.newbedwars.arena.player.inventory;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArmorTierTest {
    private final List<ArmorTier> added = new ArrayList<>();

    @AfterEach
    void tearDown() {
        added.forEach(ArmorTier::unregister);
    }

    private ArmorTier register(String name, int rank) {
        ArmorTier tier = ArmorTier.register(name, rank, Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET);
        added.add(tier);
        return tier;
    }

    @Test
    void builtInRanksAreOrdered() {
        assertTrue(ArmorTier.BASIC.rank() < ArmorTier.GOLD.rank());
        assertTrue(ArmorTier.GOLD.rank() < ArmorTier.CHAINMAIL.rank());
        assertTrue(ArmorTier.CHAINMAIL.rank() < ArmorTier.IRON.rank());
        assertTrue(ArmorTier.IRON.rank() < ArmorTier.DIAMOND.rank());
        assertTrue(ArmorTier.DIAMOND.rank() < ArmorTier.NETHERITE.rank());
    }

    @Test
    void materialsMapToTheirTier() {
        assertSame(ArmorTier.BASIC, ArmorTier.getArmorTier(Material.LEATHER_HELMET));
        assertSame(ArmorTier.GOLD, ArmorTier.getArmorTier(Material.GOLDEN_BOOTS));
        assertSame(ArmorTier.CHAINMAIL, ArmorTier.getArmorTier(Material.CHAINMAIL_CHESTPLATE));
        assertSame(ArmorTier.IRON, ArmorTier.getArmorTier(Material.IRON_LEGGINGS));
        assertSame(ArmorTier.DIAMOND, ArmorTier.getArmorTier(Material.DIAMOND_HELMET));
        assertSame(ArmorTier.NETHERITE, ArmorTier.getArmorTier(Material.NETHERITE_BOOTS));
        assertSame(ArmorTier.OTHER, ArmorTier.getArmorTier(Material.TURTLE_HELMET));
        assertSame(ArmorTier.OTHER, ArmorTier.getArmorTier(Material.STONE));
    }

    @Test
    void otherIsNeverBeatenByRank() {
        assertTrue(ArmorTier.OTHER.rank() > ArmorTier.NETHERITE.rank());
    }

    @Test
    void registeredTierIsListedAndUnregistered() {
        ArmorTier tier = register("TEST_COPPER", 10);
        assertTrue(ArmorTier.values().contains(tier));
        assertEquals("TEST_COPPER", tier.name());
        assertTrue(ArmorTier.unregister(tier));
        assertFalse(ArmorTier.values().contains(tier));
    }

    @Test
    void builtInTiersAndOtherCanNotBeRemoved() {
        assertFalse(ArmorTier.unregister(ArmorTier.IRON));
        assertFalse(ArmorTier.unregister(ArmorTier.OTHER));
        assertFalse(ArmorTier.unregister(null));
        assertTrue(ArmorTier.values().contains(ArmorTier.IRON));
    }

    @Test
    void duplicateAndReservedNamesAreRejected() {
        register("TEST_DUP", 11);
        assertThrows(IllegalArgumentException.class, () -> register("test_dup", 12));
        assertThrows(IllegalArgumentException.class, () -> register("IRON", 12));
        assertThrows(IllegalArgumentException.class, () -> register("OTHER", 12));
        assertThrows(IllegalArgumentException.class, () -> register(" ", 12));
    }

    @Test
    void piecesMustFitTheirSlot() {
        assertThrows(IllegalArgumentException.class, () -> ArmorTier.register("TEST_BAD", 13,
                Material.IRON_HELMET, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET));
        assertThrows(IllegalArgumentException.class, () -> ArmorTier.register("TEST_BAD2", 13,
                null, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET));
    }

    @Test
    void registeredTierIsFoundByItsMaterials() {
        ArmorTier tier = ArmorTier.register("TEST_TURTLE", 14, Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS,
                Material.LEATHER_CHESTPLATE, Material.TURTLE_HELMET.name().endsWith("_HELMET") ? Material.TURTLE_HELMET : Material.LEATHER_HELMET);
        added.add(tier);
        assertSame(tier.helmet() == Material.TURTLE_HELMET ? tier : ArmorTier.BASIC, ArmorTier.getArmorTier(tier.helmet()));
    }
}
