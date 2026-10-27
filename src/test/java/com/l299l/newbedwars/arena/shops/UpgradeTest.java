package com.l299l.newbedwars.arena.shops;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpgradeTest {
    private final List<Upgrade> added = new ArrayList<>();

    @AfterEach
    void tearDown() {
        added.forEach(Upgrade::unregister);
    }

    private Upgrade register(String name, int max, UpgradeHandler handler) {
        Upgrade upgrade = Upgrade.register(name, max, handler, List.of(1, 2));
        added.add(upgrade);
        return upgrade;
    }

    @Test
    void builtInUpgradesKeepTheirNamesAndLevels() {
        assertEquals(5, Upgrade.PROTECTION.maxLevel);
        assertEquals(4, Upgrade.SHARPNESS.maxLevel);
        assertEquals(2, Upgrade.HASTE.maxLevel);
        assertEquals(8, Upgrade.FORGE.maxLevel);
        assertEquals(1, Upgrade.HEALPOOL.maxLevel);
        assertEquals(2, Upgrade.ALARMTRAP.maxLevel);
        assertEquals("MININGFATIGUETRAP", Upgrade.MININGFATIGUETRAP.name());
        assertEquals(2, Upgrade.DRAGONBUFF.maxLevel);
        assertSame(Upgrade.SHARPNESS, Upgrade.valueOf("sharpness"));
        assertTrue(Upgrade.values().containsAll(List.of(Upgrade.PROTECTION, Upgrade.DRAGONBUFF, Upgrade.CUSTOMTRAP, Upgrade.CUSTOMUPGRADE)));
    }

    @Test
    void trapsAreTrapsAndUpgradesAreNot() {
        assertTrue(Upgrade.ALARMTRAP.handler().isTrap());
        assertTrue(Upgrade.BLINDTRAP.handler().isTrap());
        assertTrue(Upgrade.MININGFATIGUETRAP.handler().isTrap());
        assertFalse(Upgrade.SHARPNESS.handler().isTrap());
        assertTrue(Upgrade.SHARPNESS.handler().reapplyOnRespawn());
        assertTrue(Upgrade.HASTE.handler().reapplyOnRespawn());
        assertFalse(Upgrade.PROTECTION.handler().reapplyOnRespawn());
    }

    @Test
    void unknownNameThrowsLikeAnEnum() {
        assertThrows(IllegalArgumentException.class, () -> Upgrade.valueOf("NOPE"));
    }

    @Test
    void registeredUpgradeCanBeFoundAndRemoved() {
        Upgrade thorns = register("TEST_THORNS", 3, new UpgradeHandler() {});
        assertSame(thorns, Upgrade.valueOf("test_thorns"));
        assertEquals(3, thorns.maxLevel);
        assertEquals(List.of(1, 2), thorns.defaultPrices());
        assertFalse(thorns.isBuiltIn());
        assertTrue(Upgrade.unregister(thorns));
        assertThrows(IllegalArgumentException.class, () -> Upgrade.valueOf("TEST_THORNS"));
    }

    @Test
    void builtInUpgradesCanNotBeRemovedOrReplaced() {
        assertFalse(Upgrade.unregister(Upgrade.PROTECTION));
        assertThrows(IllegalArgumentException.class, () -> Upgrade.register("protection", 1, new UpgradeHandler() {}, List.of()));
        assertThrows(IllegalArgumentException.class, () -> Upgrade.register(" ", 1, new UpgradeHandler() {}, List.of()));
    }

    // ── TeamUpgrades with handlers ────────────────────────────────────────────

    private Team teamWith(Player... players) {
        Team team = mock(Team.class);
        when(team.getPlayers()).thenReturn(new ArrayList<>(List.of(players)));
        return team;
    }

    @Test
    void purchaseRaisesTheLevelAndAppliesTheEffectToEveryMember() {
        List<String> applied = new ArrayList<>();
        Upgrade upgrade = register("TEST_EFFECT", 2, new UpgradeHandler() {
            @Override
            public void apply(Player player, Team team, int level) {
                applied.add(player.getName() + ":" + level);
            }
        });
        Player a = mock(Player.class), b = mock(Player.class);
        when(a.getName()).thenReturn("a");
        when(b.getName()).thenReturn("b");
        TeamUpgrades upgrades = new TeamUpgrades(null, EntityType.VILLAGER, teamWith(a, b));
        upgrades.upgrade(upgrade);
        upgrades.upgrade(upgrade);
        assertEquals(2, upgrades.getUpgradeLevel(upgrade));
        assertEquals(List.of("a:1", "b:1", "a:2", "b:2"), applied);
    }

    @Test
    void handlerGetsTheNewLevelOnPurchase() {
        List<Integer> levels = new ArrayList<>();
        Upgrade upgrade = register("TEST_PURCHASE", 3, new UpgradeHandler() {
            @Override
            public void onPurchase(TeamUpgrades upgrades, Team team, int newLevel) {
                levels.add(newLevel);
            }
        });
        TeamUpgrades upgrades = new TeamUpgrades(null, EntityType.VILLAGER, teamWith());
        upgrades.upgrade(upgrade);
        upgrades.upgrade(upgrade);
        assertEquals(List.of(1, 2), levels);
    }

    @Test
    void trapsDoNothingOnPurchaseAndAreUsedUpWhenTriggered() {
        List<String> triggered = new ArrayList<>();
        Upgrade trap = register("TEST_TRAP", 2, new UpgradeHandler() {
            @Override
            public boolean isTrap() {
                return true;
            }

            @Override
            public void onPurchase(TeamUpgrades upgrades, Team team, int newLevel) {
                fail("a trap must not act on purchase");
            }

            @Override
            public void trigger(Player enemy, Team team, IArena arena) {
                triggered.add(enemy.getName());
            }
        });
        Player enemy = mock(Player.class);
        when(enemy.getName()).thenReturn("enemy");
        TeamUpgrades upgrades = new TeamUpgrades(null, EntityType.VILLAGER, teamWith());
        upgrades.upgrade(trap);
        upgrades.upgrade(trap);
        upgrades.triggerTraps(enemy, mock(IArena.class));
        assertEquals(1, upgrades.getUpgradeLevel(trap));
        upgrades.triggerTraps(enemy, mock(IArena.class));
        upgrades.triggerTraps(enemy, mock(IArena.class));
        assertEquals(0, upgrades.getUpgradeLevel(trap));
        assertEquals(List.of("enemy", "enemy"), triggered);
    }

    @Test
    void respawnReappliesOnlyTheFlaggedUpgradesThatWereBought() {
        List<String> applied = new ArrayList<>();
        Upgrade sticky = register("TEST_STICKY", 2, new UpgradeHandler() {
            @Override
            public void apply(Player player, Team team, int level) {
                applied.add("sticky" + level);
            }

            @Override
            public boolean reapplyOnRespawn() {
                return true;
            }
        });
        Upgrade once = register("TEST_ONCE", 2, new UpgradeHandler() {
            @Override
            public void apply(Player player, Team team, int level) {
                applied.add("once" + level);
            }
        });
        Player player = mock(Player.class);
        TeamUpgrades upgrades = new TeamUpgrades(null, EntityType.VILLAGER, teamWith());
        upgrades.applyPlayerUpgrades(player);
        assertTrue(applied.isEmpty());
        upgrades.upgrade(sticky);
        upgrades.upgrade(once);
        upgrades.applyPlayerUpgrades(player);
        assertEquals(List.of("sticky1"), applied);
    }

    @Test
    void unknownUpgradeHasLevelZero() {
        TeamUpgrades upgrades = new TeamUpgrades(null, EntityType.VILLAGER, teamWith());
        Upgrade late = register("TEST_LATE", 1, new UpgradeHandler() {});
        assertEquals(0, upgrades.getUpgradeLevel(late));
    }
}
