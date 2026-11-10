package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.maintenance.ArenaBackup;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult.Code;
import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService.Outcome;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiMaintenanceTest {

    @TempDir
    Path tempDir;

    private NewBedwars savedPlugin;
    private MockedStatic<Bukkit> bukkit;
    private final ApiMaintenance maintenance = new ApiMaintenance();

    @BeforeEach
    void setUp() {
        savedPlugin = NewBedwars.plugin;
        NewBedwars plugin = mock(NewBedwars.class);
        Messages messages = mock(Messages.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getMessages()).thenReturn(messages);
        when(messages.getMsgToConsole(any())).thenAnswer(i -> "§b[NewBedwars] §8> §a" + i.getArgument(0) + " §e/arenaname/§7 /info/");
        NewBedwars.plugin = plugin;
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        ArenaRegistry.clear();
        NewBedwars.plugin = savedPlugin;
    }

    private IArena arena(String name, GameStatus status, boolean enabled) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn(name);
        when(arena.status()).thenReturn(status);
        when(arena.isEnabled()).thenReturn(enabled);
        when(arena.getPlayers()).thenReturn(Set.of());
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        ArenaRegistry.register(name, arena);
        return arena;
    }

    @Test
    void resultCarriesThePlainConsoleMessage() {
        MaintenanceResult r = ApiMaintenance.toResult(new Outcome(Code.OK, "BackupCreated", "castle", "20260101"));
        assertEquals(new MaintenanceResult(Code.OK, "castle", "[NewBedwars] > BackupCreated castle 20260101", "20260101"), r);
        assertTrue(r.isSuccess());
        assertFalse(ApiMaintenance.toResult(new Outcome(Code.BUSY, "ArenaBusy", "castle", null)).isSuccess());
    }

    @Test
    void runsDirectlyOnTheServerThread() {
        IArena arena = arena("castle", GameStatus.waiting, true);
        CompletableFuture<MaintenanceResult> f = maintenance.disable("castle");
        assertTrue(f.isDone());
        assertEquals(Code.OK, f.join().code());
        verify(arena).disable();
    }

    @Test
    void otherThreadsAreMovedToTheServerThread() {
        IArena arena = arena("castle", GameStatus.waiting, true);
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        CompletableFuture<MaintenanceResult> f = maintenance.disable("castle");
        assertFalse(f.isDone());
        verify(arena, never()).disable();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(any(Plugin.class), task.capture());
        task.getValue().run();
        assertEquals(Code.OK, f.join().code());
        verify(arena).disable();
    }

    @Test
    void stoppedSchedulerFailsTheFuture() {
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenThrow(new IllegalStateException("disabled"));
        CompletableFuture<MaintenanceResult> f = maintenance.enable("castle");
        assertTrue(f.isCompletedExceptionally());
    }

    @Test
    void listBackupsOfAnUnknownArenaFails() throws Exception {
        CompletionException e = assertThrows(CompletionException.class, () -> maintenance.listBackups("nope").join());
        assertInstanceOf(NoSuchElementException.class, e.getCause());

        arena("castle", GameStatus.waiting, false);
        Path dir = Files.createDirectories(tempDir.resolve("data/backups/arenas/castle"));
        Files.writeString(dir.resolve("20260101-000000-000.json"), "{}");
        assertEquals(List.of(new ArenaBackup("20260101-000000-000", true, false)), maintenance.listBackups("castle").join());
        assertEquals(tempDir.resolve("data/exports").toAbsolutePath().normalize(), maintenance.getExportFolder());
    }

    @Test
    void arenaViewShowsLoadProblems() {
        IArena arena = arena("castle", GameStatus.restarting, false);
        List<String> problems = new ArrayList<>(List.of("team Red has no bed"));
        when(arena.getLoadProblems()).thenReturn(problems);
        List<String> seen = new ApiArena(arena).getLoadProblems();
        assertEquals(List.of("team Red has no bed"), seen);
        assertThrows(UnsupportedOperationException.class, () -> seen.add("x"));
    }
}
