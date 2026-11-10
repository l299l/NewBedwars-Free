package com.l299l.newbedwars.arena.maintenance;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult.Code;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService.Backup;
import com.l299l.newbedwars.arena.maintenance.ArenaMaintenanceService.Outcome;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArenaMaintenanceServiceTest {

    @TempDir
    Path tempDir;

    private NewBedwars savedPlugin;
    private Boolean savedLegacy;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        savedPlugin = NewBedwars.plugin;
        savedLegacy = Properties.RollbackLegacySystem;
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        NewBedwars.plugin = plugin;
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(tempDir.resolve("worlds").toFile());
        bukkit.when(Bukkit::getWorlds).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        ArenaRegistry.clear();
        Properties.RollbackLegacySystem = savedLegacy;
        NewBedwars.plugin = savedPlugin;
    }

    private IArena arena(String name, GameStatus status, boolean enabled) {
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn(name);
        when(arena.status()).thenReturn(status);
        when(arena.isEnabled()).thenReturn(enabled);
        when(arena.getPlayers()).thenReturn(Set.of());
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        when(arena.toJson()).thenReturn("{\"arenaName\":\"" + name + "\",\"worldName\":\"" + name + "\"}");
        ArenaRegistry.register(name, arena);
        return arena;
    }

    private static Outcome now(CompletableFuture<Outcome> future) {
        assertTrue(future.isDone(), "expected a result without waiting");
        return future.join();
    }

    private static void assertOutcome(Code code, String key, Outcome outcome) {
        assertEquals(code, outcome.code(), outcome.toString());
        assertEquals(key, outcome.key(), outcome.toString());
    }

    @Test
    void unknownArenaIsNotFound() {
        for (CompletableFuture<Outcome> f : List.of(
                ArenaMaintenanceService.enable("nope"),
                ArenaMaintenanceService.disable("nope"),
                ArenaMaintenanceService.backup("nope"),
                ArenaMaintenanceService.restore("nope", null),
                ArenaMaintenanceService.export("nope"),
                ArenaMaintenanceService.copy("nope", "other"),
                ArenaMaintenanceService.resetMap("nope"),
                ArenaMaintenanceService.delete("nope"))) {
            Outcome o = now(f);
            assertOutcome(Code.NOT_FOUND, "ArenaNotExists", o);
            assertEquals("nope", o.arena());
        }
    }

    @Test
    void enableChecksStateAndSetup() {
        Properties.RollbackLegacySystem = true;
        IArena enabled = arena("on", GameStatus.waiting, true);
        assertOutcome(Code.ALREADY_DONE, "AlreadyEnabled", now(ArenaMaintenanceService.enable("on")));
        verify(enabled, never()).enable();

        IArena unfinished = arena("half", GameStatus.restarting, false);
        when(unfinished.canBeEnabled()).thenReturn(false);
        assertOutcome(Code.NOT_READY, "ArenaCantBeEnabled", now(ArenaMaintenanceService.enable("half")));
        verify(unfinished, never()).enable();

        IArena ready = arena("ready", GameStatus.restarting, false);
        when(ready.canBeEnabled()).thenReturn(true);
        assertOutcome(Code.OK, "ArenaEnabled", now(ArenaMaintenanceService.enable("ready")));
        verify(ready).enable();
    }

    @Test
    void disableOnlyWhileWaiting() {
        arena("off", GameStatus.restarting, false);
        assertOutcome(Code.ALREADY_DONE, "AlreadyDisabled", now(ArenaMaintenanceService.disable("off")));

        IArena playing = arena("playing", GameStatus.playing, true);
        assertOutcome(Code.BUSY, "ArenaCantBeDisabled", now(ArenaMaintenanceService.disable("playing")));
        verify(playing, never()).disable();

        IArena waiting = arena("waiting", GameStatus.waiting, true);
        assertOutcome(Code.OK, "ArenaDisabled", now(ArenaMaintenanceService.disable("waiting")));
        verify(waiting).disable();
    }

    @Test
    void busyArenaIsLeftAlone() {
        IArena arena = arena("castle", GameStatus.waiting, true);
        when(arena.getPlayers()).thenReturn(Set.of(UUID.randomUUID()));
        for (CompletableFuture<Outcome> f : List.of(
                ArenaMaintenanceService.backup("castle"),
                ArenaMaintenanceService.restore("castle", null),
                ArenaMaintenanceService.export("castle"),
                ArenaMaintenanceService.resetMap("castle"))) {
            assertOutcome(Code.BUSY, "ArenaBusy", now(f));
        }
        assertFalse(Files.exists(tempDir.resolve("data/backups")));
        assertFalse(Files.exists(tempDir.resolve("data/exports")));
    }

    @Test
    void backupWithoutMapWritesTheArenaFile() throws IOException {
        arena("castle", GameStatus.restarting, false);
        Outcome o = now(ArenaMaintenanceService.backup("castle"));
        assertOutcome(Code.OK, "BackupCreated", o);
        Path file = tempDir.resolve("data/backups/arenas/castle").resolve(o.detail() + ".json");
        assertTrue(Files.readString(file).contains("\"arenaName\""));
        assertEquals(List.of(new Backup(o.detail(), true, false)), ArenaMaintenanceService.backups("castle"));
    }

    @Test
    void backupsAreListedNewestFirstWithTheirParts() throws IOException {
        Path arenas = Files.createDirectories(tempDir.resolve("data/backups/arenas/castle"));
        Path maps = Files.createDirectories(tempDir.resolve("data/backups/maps/castle"));
        Files.writeString(arenas.resolve("20260101-000000-000.json"), "{}");
        Files.writeString(arenas.resolve("20260102-000000-000.json"), "{}");
        Files.writeString(maps.resolve("20260102-000000-000.zip"), "");
        Files.writeString(maps.resolve("20260103-000000-000.zip"), "");
        Files.writeString(maps.resolve("notes.txt"), "");
        assertEquals(List.of(
                new Backup("20260103-000000-000", false, true),
                new Backup("20260102-000000-000", true, true),
                new Backup("20260101-000000-000", true, false)), ArenaMaintenanceService.backups("castle"));
        assertEquals(List.of(), ArenaMaintenanceService.backups("other"));
    }

    @Test
    void restoreNeedsAMatchingSafeBackup() throws IOException {
        arena("castle", GameStatus.restarting, false);
        assertOutcome(Code.NOT_FOUND, "RestoreNothing", now(ArenaMaintenanceService.restore("castle", null)));
        Files.createDirectories(tempDir.resolve("data/backups/arenas"));
        Files.writeString(tempDir.resolve("data/backups/arenas/secret.json"), "{}");
        Outcome traversal = now(ArenaMaintenanceService.restore("castle", "../secret"));
        assertOutcome(Code.NOT_FOUND, "RestoreNothing", traversal);
        assertEquals("../secret", traversal.detail());
    }

    @Test
    void copyChecksTheNewName() {
        arena("castle", GameStatus.restarting, false);
        arena("taken", GameStatus.restarting, false);
        Outcome bad = now(ArenaMaintenanceService.copy("castle", "a b"));
        assertOutcome(Code.INVALID_NAME, "CopyFailed", bad);
        assertEquals("a b", bad.arena());
        assertOutcome(Code.NAME_TAKEN, "CopyFailed", now(ArenaMaintenanceService.copy("castle", "TAKEN")));
    }

    @Test
    void deleteIsRefusedDuringAGame() {
        for (GameStatus status : List.of(GameStatus.starting, GameStatus.playing, GameStatus.ending)) {
            IArena arena = arena("castle", status, true);
            assertOutcome(Code.BUSY, "ArenaCannotBeDeleted", now(ArenaMaintenanceService.delete("castle")));
            verify(arena, never()).getArenaWorld();
        }
    }

    @Test
    void outcomeReplacementsAreNeverNull() {
        Outcome o = new Outcome(Code.OK, "X", null, null);
        assertEquals(Map.of("/arenaname/", "", "/info/", ""), o.replacements());
        assertTrue(o.success());
        assertFalse(new Outcome(Code.PARTIAL, "X", "a", "b").success());
    }

    @Test
    void exportFilesListsOnlyZips() throws IOException {
        assertEquals(List.of(), ArenaMaintenanceService.exportFiles());
        Path dir = Files.createDirectories(tempDir.resolve("data/exports"));
        Files.writeString(dir.resolve("b.zip"), "");
        Files.writeString(dir.resolve("a.zip"), "");
        Files.writeString(dir.resolve("readme.txt"), "");
        assertEquals(List.of("a.zip", "b.zip"), ArenaMaintenanceService.exportFiles());
        assertEquals(dir.toAbsolutePath().normalize(), ArenaMaintenanceService.exportsDir());
    }

    @Test
    void transferHelpers() throws IOException {
        assertTrue(ArenaTransfer.isSafeName("castle_2-x"));
        assertFalse(ArenaTransfer.isSafeName("../x"));
        assertFalse(ArenaTransfer.isSafeName(""));
        assertFalse(ArenaTransfer.isSafeName(null));
        Path template = tempDir.resolve("t");
        Files.createDirectories(template.resolve("data/paper"));
        Files.writeString(template.resolve("uid.dat"), "x");
        Files.writeString(template.resolve("data/paper/metadata.dat"), "x");
        Files.writeString(template.resolve("level.dat"), "x");
        ArenaTransfer.stripWorldIdentity(template);
        assertFalse(Files.exists(template.resolve("uid.dat")));
        assertFalse(Files.exists(template.resolve("data/paper/metadata.dat")));
        assertTrue(Files.exists(template.resolve("level.dat")));
        ArenaTransfer.stripWorldIdentity(template);
    }
}
