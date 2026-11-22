package com.l299l.newbedwars.arena.shops;

import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.IArena;

import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TeamUpgradesLevelTest {
    private final List<Integer> purchases = new ArrayList<>();
    private int triggers;
    private Upgrade upgrade;
    private Upgrade trap;
    private TeamUpgrades upgrades;

    @BeforeEach
    void setUp() {
        upgrade = Upgrade.register("TESTLEVELUPGRADE", 4, new UpgradeHandler() {
            @Override
            public void onPurchase(TeamUpgrades u, Team team, int newLevel) {
                purchases.add(newLevel);
            }
        }, List.of(1, 2, 3, 4));
        trap = Upgrade.register("TESTLEVELTRAP", 3, new UpgradeHandler() {
            @Override
            public boolean isTrap() {
                return true;
            }

            @Override
            public void trigger(Player enemy, Team team, IArena arena) {
                triggers++;
            }
        }, List.of(1, 1, 1));
        Team team = mock(Team.class);
        upgrades = new TeamUpgrades(null, EntityType.VILLAGER, team);
    }

    @AfterEach
    void tearDown() {
        Upgrade.unregister(upgrade);
        Upgrade.unregister(trap);
    }

    @Test
    void raisingAnUpgradeAppliesEveryLevel() {
        assertTrue(upgrades.setLevel(upgrade, 3));
        assertEquals(3, upgrades.getUpgradeLevel(upgrade));
        assertEquals(List.of(1, 2, 3), purchases);
    }

    @Test
    void upgradesCanNotBeLowered() {
        upgrades.setLevel(upgrade, 2);
        assertFalse(upgrades.setLevel(upgrade, 1));
        assertEquals(2, upgrades.getUpgradeLevel(upgrade));
        assertTrue(upgrades.setLevel(upgrade, 2));
    }

    @Test
    void trapsCanBeSetToAnyCount() {
        assertTrue(upgrades.setLevel(trap, 3));
        assertTrue(upgrades.setLevel(trap, 0));
        assertEquals(0, upgrades.getUpgradeLevel(trap));
    }

    @Test
    void aCancelledTrapIsKept() {
        upgrades.setLevel(trap, 1);
        try (MockedStatic<ApiEvents> events = mockStatic(ApiEvents.class)) {
            events.when(() -> ApiEvents.trapTrigger(any(), any(), any(), anyString())).thenReturn(false);
            upgrades.triggerTraps(mock(Player.class), mock(IArena.class));
            assertEquals(1, upgrades.getUpgradeLevel(trap));
            assertEquals(0, triggers);

            events.when(() -> ApiEvents.trapTrigger(any(), any(), any(), anyString())).thenReturn(true);
            upgrades.triggerTraps(mock(Player.class), mock(IArena.class));
            assertEquals(0, upgrades.getUpgradeLevel(trap));
            assertEquals(1, triggers);
        }
    }
}
