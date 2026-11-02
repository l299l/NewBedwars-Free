package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.data.DataManager;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArenaMaintenanceCommandTest {

    @TempDir
    Path tempDir;

    private NewBedwars savedPlugin;
    private Messages messages;
    private ArenaDataManager arenaData;
    private Player player;
    private MockedStatic<Bukkit> bukkit;
    private ArenaMaintenanceCommand command;

    @BeforeEach
    void setUp() throws IOException {
        savedPlugin = NewBedwars.plugin;
        NewBedwars plugin = mock(NewBedwars.class);
        messages = mock(Messages.class);
        DataManager dataManager = mock(DataManager.class);
        arenaData = mock(ArenaDataManager.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getDataManager()).thenReturn(dataManager);
        when(dataManager.getArenaDataManager()).thenReturn(arenaData);
        NewBedwars.plugin = plugin;
        player = mock(Player.class);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(tempDir.resolve("worlds").toFile());
        bukkit.when(Bukkit::getWorlds).thenReturn(Collections.emptyList());
        Files.createDirectories(tempDir.resolve("data/exports"));
        command = new ArenaMaintenanceCommand(ArenaMaintenanceCommand.Action.IMPORT);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        NewBedwars.plugin = savedPlugin;
    }

    private Path exportZip(String fileName, Map<String, String> entries) throws IOException {
        Path zip = tempDir.resolve("data/exports").resolve(fileName);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return zip;
    }

    private Map<String, String> validEntries(String arena, String world) {
        return Map.of(
                "arena.json", "{\"arenaName\":\"" + arena + "\",\"worldName\":\"" + world + "\",\"enabled\":true}",
                "template/level.dat", "level",
                "template/uid.dat", "uid",
                "template/data/paper/metadata.dat", "meta");
    }

    private void assertReply(String key) {
        verify(messages).send(eq(player), eq(key), ArgumentMatchers.<Map<String, String>>any());
    }

    private boolean importTempLeft() throws IOException {
        try (Stream<Path> s = Files.list(tempDir.resolve("data/exports"))) {
            return s.anyMatch(p -> p.getFileName().toString().startsWith(".import-"));
        }
    }

    @Test
    void importRejectsPathTraversalFileName() throws IOException {
        exportZip("a.zip", validEntries("castle", "castle"));
        Files.copy(tempDir.resolve("data/exports/a.zip"), tempDir.resolve("outside.zip"));
        command.doImport(player, "../../outside.zip");
        assertReply("ImportFailed");
        verifyNoInteractions(arenaData);
    }

    @Test
    void importRejectsUnsafeNamesAndCleansUp() throws IOException {
        exportZip("bad.zip", validEntries("../evil", "castle"));
        command.doImport(player, "bad.zip");
        assertReply("ImportFailed");
        verifyNoInteractions(arenaData);
        assertFalse(importTempLeft());
    }

    @Test
    void importRejectsExistingArenaFile() throws IOException {
        Files.createDirectories(tempDir.resolve("data/arenas"));
        Files.writeString(tempDir.resolve("data/arenas/castle.json"), "{}");
        exportZip("c.zip", validEntries("castle", "castle"));
        command.doImport(player, "c.zip");
        assertReply("ImportFailed");
        assertEquals("{}", Files.readString(tempDir.resolve("data/arenas/castle.json")));
        assertFalse(importTempLeft());
    }

    @Test
    void importWithoutMapFails() throws IOException {
        exportZip("nomap.zip", Map.of("arena.json", "{\"arenaName\":\"castle\",\"worldName\":\"castle\"}"));
        command.doImport(player, "nomap.zip");
        assertReply("ImportFailed");
        assertFalse(Files.exists(tempDir.resolve("data/arenas/castle.json")));
    }

    @Test
    void importLoadsDisabledArenaWithoutWorldIdentityFiles() throws IOException {
        exportZip("ok.zip", validEntries("castle", "castle"));
        IArena loaded = mock(IArena.class);
        when(arenaData.loadArena("castle")).thenReturn(loaded);
        command.doImport(player, "ok.zip");
        assertReply("ImportDone");
        String json = Files.readString(tempDir.resolve("data/arenas/castle.json"));
        assertFalse(com.google.gson.JsonParser.parseString(json).getAsJsonObject().get("enabled").getAsBoolean());
        Path template = tempDir.resolve("data/maps/castle/template");
        assertTrue(Files.exists(template.resolve("level.dat")));
        assertFalse(Files.exists(template.resolve("uid.dat")));
        assertFalse(Files.exists(template.resolve("data/paper/metadata.dat")));
        assertFalse(importTempLeft());
    }

    @Test
    void importRollsBackWhenTheArenaCannotBeLoaded() throws IOException {
        exportZip("fail.zip", validEntries("castle", "castle"));
        when(arenaData.loadArena("castle")).thenReturn(null);
        command.doImport(player, "fail.zip");
        assertReply("ImportFailed");
        assertFalse(Files.exists(tempDir.resolve("data/arenas/castle.json")));
        assertFalse(Files.exists(tempDir.resolve("data/maps/castle")));
        assertFalse(Files.exists(tempDir.resolve("worlds/castle")));
        assertFalse(importTempLeft());
    }

    @Test
    void importRollsBackWhenLoadingThrows() throws IOException {
        exportZip("boom.zip", validEntries("castle", "castle"));
        when(arenaData.loadArena("castle")).thenThrow(new IllegalStateException("boom"));
        command.doImport(player, "boom.zip");
        assertReply("ImportFailed");
        assertFalse(Files.exists(tempDir.resolve("data/arenas/castle.json")));
        assertFalse(Files.exists(tempDir.resolve("data/maps/castle")));
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
