package com.l299l.newbedwars.arena.player.inventory;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArmorContentsTest {
    @BeforeEach
    void setUp() {
        NewBedwars.plugin = mock(NewBedwars.class);
        when(NewBedwars.plugin.getCustomItemManager()).thenReturn(mock(CustomItemManager.class));
        when(NewBedwars.plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void missingBasicSwordDoesNotCrash() {
        ArmorContents contents = new ArmorContents(mock(Player.class), mock(Team.class));
        assertFalse(contents.hasBoughtSword());
        assertDoesNotThrow(contents::resetSword);
    }
}
