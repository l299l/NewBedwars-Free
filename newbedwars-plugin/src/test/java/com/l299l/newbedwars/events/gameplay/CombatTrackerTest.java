package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.NewBedwars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CombatTrackerTest {
    private final UUID victim = UUID.randomUUID();
    private final UUID attacker = UUID.randomUUID();
    private MockedStatic<Bukkit> bukkit;
    private BukkitScheduler scheduler;
    private BukkitTask task;

    @BeforeEach
    void setUp() {
        NewBedwars.plugin = mock(NewBedwars.class);
        scheduler = mock(BukkitScheduler.class);
        task = mock(BukkitTask.class);
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(task);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        NewBedwars.plugin = null;
    }

    @Test
    void lastHitIsForgottenAfterTheTimer() {
        CombatTracker combat = new CombatTracker();
        combat.record(victim, attacker, true);
        assertEquals(attacker, combat.lastAttacker(victim));

        ArgumentCaptor<Runnable> clear = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(any(), clear.capture(), eq(200L));
        clear.getValue().run();
        assertNull(combat.lastAttacker(victim));
        assertFalse(combat.takeMobHit(victim));
    }

    @Test
    void aPlayerHitReplacesTheMobFlagAndTheTimer() {
        CombatTracker combat = new CombatTracker();
        combat.record(victim, attacker, true);
        combat.record(victim, attacker, false);
        verify(task).cancel();
        assertFalse(combat.takeMobHit(victim));
    }

    @Test
    void takeMethodsReturnOnce() {
        CombatTracker combat = new CombatTracker();
        Location ground = new Location(null, 1, 2, 3);
        combat.setGround(victim, ground);
        combat.markVoidDeath(victim);
        assertSame(ground, combat.takeGround(victim));
        assertNull(combat.takeGround(victim));
        assertTrue(combat.takeVoidDeath(victim));
        assertFalse(combat.takeVoidDeath(victim));
    }

    @Test
    void forgetClearsEverything() {
        CombatTracker combat = new CombatTracker();
        combat.record(victim, attacker, true);
        combat.markVoidDeath(victim);
        combat.setGround(victim, new Location(null, 0, 0, 0));
        combat.forget(victim);
        verify(task).cancel();
        assertNull(combat.lastAttacker(victim));
        assertFalse(combat.takeMobHit(victim));
        assertFalse(combat.takeVoidDeath(victim));
        assertNull(combat.takeGround(victim));
    }
}
