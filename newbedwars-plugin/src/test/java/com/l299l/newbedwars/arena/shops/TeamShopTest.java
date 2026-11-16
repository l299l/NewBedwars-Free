package com.l299l.newbedwars.arena.shops;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TeamShopTest {

    @Test
    void npcThatMovedMoreThanHalfABlockHasDrifted() {
        World world = mock(World.class);
        Location home = new Location(world, 10.5, 64, 10.5);
        assertFalse(TeamShop.drifted(new Location(world, 10.5, 64, 10.5, 90, 0), home));
        assertFalse(TeamShop.drifted(new Location(world, 10.8, 64, 10.5), home));
        assertTrue(TeamShop.drifted(new Location(world, 11.5, 64, 10.5), home));
        assertTrue(TeamShop.drifted(new Location(mock(World.class), 10.5, 64, 10.5), home));
    }
}
