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
    void forceStopWorksWhileTheArenaIsInUse() {
        when(arena.isEnabled()).thenReturn(true);
        for (GameStatus s : List.of(GameStatus.waiting, GameStatus.starting, GameStatus.playing, GameStatus.ending)) {
            assertTrue(run(s, Action.FORCE_STOP).success());
        }
        verify(arena, times(4)).broadcast(eq("AdminStoppedGame"), any());
        verify(arena, times(4)).stop();
    }

    @Test
    void forceStopIsRefusedForAResettingOrDisabledArena() {
        when(arena.isEnabled()).thenReturn(true);
        Outcome resetting = run(GameStatus.restarting, Action.FORCE_STOP);
        assertEquals(Code.BUSY, resetting.code());
        assertEquals("ArenaBusy", resetting.key());
        assertEquals("Castle", resetting.replacements().get("/arenaname/"));
        when(arena.isEnabled()).thenReturn(false);
        assertEquals(Code.BUSY, run(GameStatus.waiting, Action.FORCE_STOP).code());
        verify(arena, never()).stop();
        verify(arena, never()).broadcast(eq("AdminStoppedGame"), any());
    }

    @Test
    void nextPhaseReportsTheNextName() {
        assertEquals(Code.NOT_READY, run(GameStatus.waiting, Action.NEXT_PHASE).code());
        when(arena.getNextGamePhase()).thenReturn("Diamond II");
        Outcome o = run(GameStatus.playing, Action.NEXT_PHASE);
        assertEquals("PhaseAdvancedTo", o.key());
        assertEquals("Diamond II", o.replacements().get("/phase/"));
        when(arena.getNextGamePhase()).thenReturn("");
        Outcome last = run(GameStatus.playing, Action.NEXT_PHASE);
        assertEquals(Code.ALREADY_DONE, last.code());
        assertEquals("AlreadyLastPhase", last.key());
        verify(arena, times(1)).advancePhase();
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

    private com.l299l.newbedwars.arena.team.Team team(String name) {
        com.l299l.newbedwars.arena.team.Team team = mock(com.l299l.newbedwars.arena.team.Team.class);
        when(team.getName()).thenReturn(name);
        when(team.getDisplayName()).thenReturn(name);
        when(team.colored(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(arena.getTeam(name)).thenReturn(team);
        when(arena.getTeams()).thenReturn(Map.of(name, team));
        return team;
    }

    @Test
    void teamActionsNeedARunningGameAndAKnownTeam() {
        team("Red");
        when(arena.status()).thenReturn(GameStatus.waiting);
        assertEquals("GameNotInProgress", GameControlService.destroyBed("Castle", "Red", "x").key());
        when(arena.status()).thenReturn(GameStatus.playing);
        assertEquals(Code.NOT_FOUND, GameControlService.eliminateTeam("Castle", "Green", "x").code());
        assertEquals("TeamNotFound", GameControlService.setUpgradeLevel("Castle", "Green", "SHARPNESS", 1, "x").key());
        assertEquals(Code.NOT_FOUND, GameControlService.destroyBed("Nope", "Red", "x").code());
    }

    @Test
    void destroyBedOnlyOnce() {
        com.l299l.newbedwars.arena.team.Team red = team("Red");
        when(arena.status()).thenReturn(GameStatus.playing);
        when(red.getTeamBed()).thenReturn(mock(com.l299l.newbedwars.arena.team.TeamBed.class));
        when(arena.destroyBed(red)).thenReturn(true);

        Outcome ok = GameControlService.destroyBed("castle", "red", "Panel: Alex");
        assertTrue(ok.success());
        assertEquals("AdminDestroyedBed", ok.key());
        assertEquals("Red", ok.replacements().get("/team/"));

        when(red.isBedDestroyed()).thenReturn(true);
        assertEquals(Code.ALREADY_DONE, GameControlService.destroyBed("Castle", "Red", "x").code());
        verify(arena, times(1)).destroyBed(red);
    }

    @Test
    void cancelledBedBreakFails() {
        com.l299l.newbedwars.arena.team.Team red = team("Red");
        when(arena.status()).thenReturn(GameStatus.playing);
        when(red.getTeamBed()).thenReturn(mock(com.l299l.newbedwars.arena.team.TeamBed.class));
        when(arena.destroyBed(red)).thenReturn(false);
        assertEquals(Code.FAILED, GameControlService.destroyBed("Castle", "Red", "x").code());
    }

    @Test
    void eliminateOnlyLivingTeams() {
        com.l299l.newbedwars.arena.team.Team red = team("Red");
        when(arena.status()).thenReturn(GameStatus.playing);
        when(red.isAlive()).thenReturn(true);
        when(arena.eliminateTeam(red)).thenReturn(true);
        assertEquals("AdminEliminatedTeam", GameControlService.eliminateTeam("Castle", "Red", "x").key());
        when(red.isAlive()).thenReturn(false);
        assertEquals(Code.ALREADY_DONE, GameControlService.eliminateTeam("Castle", "Red", "x").code());
    }

    @Test
    void upgradeLevelsAreCheckedAndApplied() {
        com.l299l.newbedwars.arena.team.Team red = team("Red");
        when(arena.status()).thenReturn(GameStatus.playing);
        com.l299l.newbedwars.arena.shops.TeamUpgrades upgrades = mock(com.l299l.newbedwars.arena.shops.TeamUpgrades.class);
        when(red.getTeamUpgrades()).thenReturn(upgrades);
        com.l299l.newbedwars.arena.shops.Upgrade sharpness = com.l299l.newbedwars.arena.shops.Upgrade.SHARPNESS;
        when(upgrades.setLevel(sharpness, 2)).thenReturn(true);

        Outcome ok = GameControlService.setUpgradeLevel("Castle", "Red", "sharpness", 2, "x");
        assertTrue(ok.success());
        assertEquals("SHARPNESS", ok.replacements().get("/upgrade/"));
        verify(upgrades).setLevel(sharpness, 2);

        assertEquals("AdminUpgradeNotFound", GameControlService.setUpgradeLevel("Castle", "Red", "NOPE", 1, "x").key());
        assertEquals("AdminUpgradeLevelInvalid",
                GameControlService.setUpgradeLevel("Castle", "Red", "SHARPNESS", sharpness.maxLevel + 1, "x").key());
        assertEquals(Code.INVALID_NAME, GameControlService.setUpgradeLevel("Castle", "Red", "SHARPNESS", -1, "x").code());
        assertEquals("AdminUpgradeCannotLower", GameControlService.setUpgradeLevel("Castle", "Red", "SHARPNESS", 1, "x").key());
    }
}
