package com.l299l.newbedwars.arena.game;

import com.l299l.newbedwars.api.maintenance.MaintenanceResult.Code;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.game.GameControlService.Action;
import com.l299l.newbedwars.arena.game.GameControlService.Outcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GameControlServiceTest {
    private IArena arena;

    @BeforeEach
    void setUp() {
        ArenaRegistry.clear();
        arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("Castle");
        ArenaRegistry.register("Castle", arena);
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
    }

    private Outcome run(GameStatus status, Action action) {
        when(arena.status()).thenReturn(status);
        return GameControlService.run("castle", action, "Panel: Alex");
    }

    @Test
    void unknownArenaIsNotFound() {
        Outcome o = GameControlService.run("nope", Action.START, "x");
        assertEquals(Code.NOT_FOUND, o.code());
        assertEquals("ArenaNotExists", o.key());
        assertEquals(Code.NOT_FOUND, GameControlService.broadcast("nope", "x", "hi").code());
    }

    @Test
    void startOnlyWhileWaiting() {
        Outcome ok = run(GameStatus.waiting, Action.START);
        assertTrue(ok.success());
        verify(arena).broadcast("AdminForcedStart", Map.of("/admin/", "Panel: Alex"));
        verify(arena).setArenaStarting();
        for (GameStatus s : List.of(GameStatus.starting, GameStatus.playing, GameStatus.ending, GameStatus.restarting)) {
            assertEquals("GameNotWaiting", run(s, Action.START).key());
        }
        verify(arena, times(1)).setArenaStarting();
    }

    @Test
    void forceStartWhileWaitingOrStarting() {
        assertTrue(run(GameStatus.waiting, Action.FORCE_START).success());
        assertTrue(run(GameStatus.starting, Action.FORCE_START).success());
        assertEquals(Code.NOT_READY, run(GameStatus.playing, Action.FORCE_START).code());
        verify(arena, times(2)).start();
    }

    @Test
    void stopIsADrawWhilePlaying() {
        assertEquals("GameNotInProgress", run(GameStatus.waiting, Action.STOP).key());
        verify(arena, never()).endGame(anyString());
        Outcome o = run(GameStatus.playing, Action.STOP);
        assertTrue(o.success());
        verify(arena).endGame("DRAW");
    }

    @Test
    void forceStopWorksInEveryState() {
        assertTrue(run(GameStatus.ending, Action.FORCE_STOP).success());
        verify(arena).broadcast(eq("AdminStoppedGame"), any());
        verify(arena).stop();
    }

    @Test
    void nextPhaseReportsTheNextName() {
        assertEquals(Code.NOT_READY, run(GameStatus.waiting, Action.NEXT_PHASE).code());
        when(arena.getNextGamePhase()).thenReturn("Diamond II");
        Outcome o = run(GameStatus.playing, Action.NEXT_PHASE);
        assertEquals("PhaseAdvancedTo", o.key());
        assertEquals("Diamond II", o.replacements().get("/phase/"));
        when(arena.getNextGamePhase()).thenReturn("");
        assertEquals("PhaseAdvanced", run(GameStatus.playing, Action.NEXT_PHASE).key());
        verify(arena, times(2)).advancePhase();
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcastTranslatesColoursAndChecksLength() {
        Outcome o = GameControlService.broadcast("Castle", "Alex", "  &eRestart soon  ");
        assertTrue(o.success());
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(arena).broadcast(eq("AdminBroadcast"), captor.capture());
        assertEquals("§eRestart soon", captor.getValue().get("/message/"));
        assertEquals(List.of("/admin/", "/message/"), List.copyOf(captor.getValue().keySet()));

        assertEquals(Code.INVALID_NAME, GameControlService.broadcast("Castle", "Alex", "   ").code());
        assertEquals(Code.INVALID_NAME, GameControlService.broadcast("Castle", "Alex", "x".repeat(257)).code());
        verify(arena, times(1)).broadcast(eq("AdminBroadcast"), any());
    }
}
