package com.l299l.newbedwars.arena.phases;

import com.l299l.newbedwars.arena.IArena;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GamePhasesTest {

    private static Phase mockPhase() {
        Phase p = mock(Phase.class);
        doNothing().when(p).start(any());
        doNothing().when(p).cancel();
        when(p.copy()).thenAnswer(i -> mockPhase());
        return p;
    }

    @Test
    void isLastPhaseReturnsTrueForSinglePhase() {
        GamePhases gp = new GamePhases("test", List.of(mockPhase()));
        assertTrue(gp.isLastPhase());
    }

    @Test
    void isLastPhaseReturnsFalseWhenMorePhasesRemain() {
        GamePhases gp = new GamePhases("test", List.of(mockPhase(), mockPhase()));
        assertFalse(gp.isLastPhase());
    }

    @Test
    void getNextPhaseReturnsNullAtLastPhase() {
        GamePhases gp = new GamePhases("test", List.of(mockPhase()));
        assertNull(gp.getNextPhase());
    }

    @Test
    void getNextPhaseReturnsSecondPhaseWhenAtFirst() {
        Phase first = mockPhase();
        Phase second = mockPhase();
        GamePhases gp = new GamePhases("test", List.of(first, second));
        assertSame(second, gp.getNextPhase());
    }

    @Test
    void startCallsFirstPhase() {
        Phase first = mockPhase();
        IArena arena = mock(IArena.class);
        GamePhases gp = new GamePhases("test", List.of(first));
        gp.start(arena);
        verify(first).start(arena);
    }

    @Test
    void nextPhaseAdvancesToSecondPhase() {
        Phase first = mockPhase();
        Phase second = mockPhase();
        IArena arena = mock(IArena.class);
        GamePhases gp = new GamePhases("test", List.of(first, second));
        gp.nextPhase(arena);
        verify(second).start(arena);
        verify(first, never()).start(any());
    }

    @Test
    void nextPhaseAtLastPhaseIsNoOp() {
        Phase only = mockPhase();
        IArena arena = mock(IArena.class);
        GamePhases gp = new GamePhases("test", List.of(only));
        assertDoesNotThrow(() -> gp.nextPhase(arena));
        verify(only, never()).start(any());
    }

    @Test
    void stopCancelsCurrentPhaseAndResetsIndex() {
        Phase first = mockPhase();
        Phase second = mockPhase();
        IArena arena = mock(IArena.class);
        GamePhases gp = new GamePhases("test", List.of(first, second));
        gp.nextPhase(arena);
        gp.stop();
        verify(second).cancel();
        assertFalse(gp.isLastPhase());
    }

    @Test
    void copyConstructorStartsAtPhaseZeroRegardlessOfOriginal() {
        Phase first = mockPhase();
        Phase second = mockPhase();
        IArena arena = mock(IArena.class);
        GamePhases original = new GamePhases("test", List.of(first, second));
        original.nextPhase(arena);
        GamePhases copy = new GamePhases(original);
        assertFalse(copy.isLastPhase());
        assertNotNull(copy.getNextPhase());
    }

    @Test
    void copyConstructorPreservesID() {
        GamePhases gp = new GamePhases("myPhase", List.of(mockPhase()));
        GamePhases copy = new GamePhases(gp);
        assertEquals("myPhase", copy.getID());
    }

    @Test
    void copyDoesNotShareRunningPhasesWithTheOriginal() {
        Phase first = new Phase("first", 30, List.of());
        GamePhases original = new GamePhases("test", List.of(first));
        GamePhases copy = new GamePhases(original);
        assertNotSame(original.getCurrentPhase(), copy.getCurrentPhase());
        assertEquals("first", copy.getCurrentPhase().getName());
    }

    @Test
    void getCurrentPhaseReturnsFirstPhaseInitially() {
        Phase first = mockPhase();
        GamePhases gp = new GamePhases("test", List.of(first, mockPhase()));
        assertSame(first, gp.getCurrentPhase());
    }

    @Test
    void getPhasesIsUnmodifiableAndKeepsOrder() {
        Phase first = mockPhase();
        Phase second = mockPhase();
        GamePhases gp = new GamePhases("test", new java.util.ArrayList<>(List.of(first, second)));
        assertEquals(List.of(first, second), gp.getPhases());
        assertThrows(UnsupportedOperationException.class, () -> gp.getPhases().add(mockPhase()));
        assertThrows(UnsupportedOperationException.class, () -> gp.getPhases().remove(0));
    }

    @Test
    void phaseExposesInitialDurationAndActions() {
        PhaseAction action = new PhaseAction("A:1");
        Phase phase = new Phase("One", 90, List.of(action));
        assertEquals(90, phase.getInitialDuration());
        assertEquals(List.of(action), phase.getActions());
        assertThrows(UnsupportedOperationException.class, () -> phase.getActions().add(action));
        assertEquals(90, phase.copy().getInitialDuration());
    }

    @Test
    void phaseNullDurationAndActionsDefaultToEmpty() {
        Phase phase = new Phase("One", null, null);
        assertEquals(0, phase.getInitialDuration());
        assertTrue(phase.getActions().isEmpty());
    }

    @Test
    void initialDurationIsUnchangedByTheCountdown() {
        Phase phase = new Phase("One", 90, List.of());
        org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        org.bukkit.scheduler.BukkitTask task = mock(org.bukkit.scheduler.BukkitTask.class);
        com.l299l.newbedwars.NewBedwars plugin = mock(com.l299l.newbedwars.NewBedwars.class);
        com.l299l.newbedwars.NewBedwars.plugin = plugin;
        try (org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenAnswer(i -> {
                Runnable r = i.getArgument(1);
                r.run();
                r.run();
                return task;
            });
            phase.start(mock(IArena.class));
            assertEquals(88, phase.getDuration());
            assertEquals(90, phase.getInitialDuration());
        } finally {
            com.l299l.newbedwars.NewBedwars.plugin = null;
        }
    }
}
