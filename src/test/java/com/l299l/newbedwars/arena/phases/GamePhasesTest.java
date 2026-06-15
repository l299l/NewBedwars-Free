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
        return p;
    }

    // ── isLastPhase ──────────────────────────────────────────────────────────

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

    // ── getNextPhase ─────────────────────────────────────────────────────────

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

    // ── start / nextPhase ────────────────────────────────────────────────────

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
        verify(only, never()).start(any()); // still at last phase, not re-started
    }

    // ── stop ─────────────────────────────────────────────────────────────────

    @Test
    void stopCancelsCurrentPhaseAndResetsIndex() {
        Phase first = mockPhase();
        Phase second = mockPhase();
        IArena arena = mock(IArena.class);
        GamePhases gp = new GamePhases("test", List.of(first, second));
        gp.nextPhase(arena);           // advance to phase index 1
        gp.stop();
        verify(second).cancel();
        assertFalse(gp.isLastPhase()); // back to index 0, which is not the last
    }

    // ── copy constructor ─────────────────────────────────────────────────────

    @Test
    void copyConstructorStartsAtPhaseZeroRegardlessOfOriginal() {
        Phase first = mockPhase();
        Phase second = mockPhase();
        IArena arena = mock(IArena.class);
        GamePhases original = new GamePhases("test", List.of(first, second));
        original.nextPhase(arena);     // original is now at phase 1
        GamePhases copy = new GamePhases(original);
        assertFalse(copy.isLastPhase()); // copy starts at phase 0
        assertSame(second, copy.getNextPhase());
    }

    @Test
    void copyConstructorPreservesID() {
        GamePhases gp = new GamePhases("myPhase", List.of(mockPhase()));
        GamePhases copy = new GamePhases(gp);
        assertEquals("myPhase", copy.getID());
    }

    // ── getCurrentPhase ──────────────────────────────────────────────────────

    @Test
    void getCurrentPhaseReturnsFirstPhaseInitially() {
        Phase first = mockPhase();
        GamePhases gp = new GamePhases("test", List.of(first, mockPhase()));
        assertSame(first, gp.getCurrentPhase());
    }
}
