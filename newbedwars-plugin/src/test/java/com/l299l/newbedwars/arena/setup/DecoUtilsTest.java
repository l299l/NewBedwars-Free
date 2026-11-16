package com.l299l.newbedwars.arena.setup;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DecoUtilsTest {

    private static ArmorStand stand(World world, Location at) {
        ArmorStand stand = mock(ArmorStand.class);
        when(stand.getLocation()).thenReturn(at);
        doAnswer(i -> {
            when(stand.getCustomName()).thenReturn(i.getArgument(0));
            return null;
        }).when(stand).setCustomName(any());
        when(world.spawnEntity(eq(at), eq(EntityType.ARMOR_STAND))).thenReturn(stand);
        return stand;
    }

    @Test
    void onlyTheMarkerWithTheSameNameIsRemoved() {
        World world = mock(World.class);
        Location spawn = new Location(world, 10, 64, 10);
        Location shop = new Location(world, 11, 64, 10);
        ArmorStand spawnStand = stand(world, spawn);
        ArmorStand shopStand = stand(world, shop);
        DecoUtils.summonSetupArmorStand(spawn, "§cSpawn");
        DecoUtils.summonSetupArmorStand(shop, "§cShop");

        DecoUtils.removeArmorStandAt(spawn, "§cSpawn");

        verify(spawnStand).remove();
        verify(shopStand, never()).remove();
        DecoUtils.removeArmorStandAt(shop, "§cShop");
        verify(shopStand).remove();
    }

    @Test
    void nearMeansSameWorldAndAtMostOneBlockAwayAndTwoInHeight() {
        World world = mock(World.class);
        World other = mock(World.class);
        Location base = new Location(world, 0, 64, 0);
        assertTrue(DecoUtils.isNear(new Location(world, 1, 66, -1), base));
        assertFalse(DecoUtils.isNear(new Location(world, 2, 64, 0), base));
        assertFalse(DecoUtils.isNear(new Location(world, 0, 67, 0), base));
        assertFalse(DecoUtils.isNear(new Location(other, 0, 64, 0), base));
    }
}
