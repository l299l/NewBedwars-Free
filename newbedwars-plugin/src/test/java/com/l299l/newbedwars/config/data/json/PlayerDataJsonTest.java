package com.l299l.newbedwars.config.data.json;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.player.PlayerProfile;
import com.l299l.newbedwars.player.PlayerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerDataJsonTest {

    @TempDir
    Path tempDir;

    private NewBedwars savedPlugin;
    private PlayerManager players;

    @BeforeEach
    void setUp() throws Exception {
        savedPlugin = NewBedwars.plugin;
        NewBedwars plugin = mock(NewBedwars.class);
        players = mock(PlayerManager.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getPlayerManager()).thenReturn(players);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(players.getPlayers()).thenReturn(new HashMap<>());
        NewBedwars.plugin = plugin;
        Files.createDirectories(tempDir.resolve("data"));
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = savedPlugin;
    }

    private File file() {
        return tempDir.resolve("data/playersData.json").toFile();
    }

    @Test
    void corruptFileIsMovedAsideAndNotOverwrittenByTheEmptyState() throws Exception {
        Files.writeString(file().toPath(), "{ this is not json");
        PlayerDataJson json = new PlayerDataJson();
        json.load();
        assertFalse(file().exists());
        try (Stream<Path> s = Files.list(tempDir.resolve("data"))) {
            Path corrupt = s.filter(p -> p.getFileName().toString().startsWith("playersData.json.corrupt-")).findFirst().orElseThrow();
            assertEquals("{ this is not json", Files.readString(corrupt));
        }
    }

    @Test
    void validFileLoadsProfiles() throws Exception {
        UUID id = UUID.randomUUID();
        Files.writeString(file().toPath(), "{\"Leo\":{\"id\":\"" + id + "\",\"name\":\"Leo\",\"language\":\"English\",\"fastBuyPerCategory\":{}}}");
        PlayerDataJson json = new PlayerDataJson();
        json.load();
        verify(players).addPlayer(argThat(p -> p.name().equals("Leo") && p.id().equals(id)));
        assertTrue(file().exists());
    }

    @Test
    void missingFileIsCreatedOnLoad() {
        new PlayerDataJson().load();
        assertTrue(file().exists());
    }

    @Test
    void asyncSavesAreCoalescedAndWrittenAtomically() throws Exception {
        when(NewBedwars.plugin.isEnabled()).thenReturn(true);
        PlayerDataJson json = new PlayerDataJson();
        HashMap<String, PlayerProfile> map = new HashMap<>();
        when(players.getPlayers()).thenReturn(map);
        UUID id = UUID.randomUUID();
        for (int i = 0; i < 20; i++) {
            map.put("Leo", new PlayerProfile(id, "Leo", i % 2 == 0 ? Language.English : Language.Polish, new HashMap<>()));
            json.save();
        }
        IOExecutor.flush();
        String written = Files.readString(file().toPath());
        assertTrue(written.contains("\"Polish\""));
        try (Stream<Path> s = Files.list(tempDir.resolve("data"))) {
            assertEquals(1, s.count());
        }
    }

    @Test
    void aCorruptFileIsKeptNextToTheFreshOne() throws Exception {
        Files.writeString(file().toPath(), "{ not json");
        PlayerDataJson json = new PlayerDataJson();
        json.load();
        json.save();
        IOExecutor.flush();
        try (Stream<Path> s = Files.list(tempDir.resolve("data"))) {
            Path kept = s.filter(p -> p.getFileName().toString().contains(".corrupt-")).findFirst().orElseThrow();
            assertEquals("{ not json", Files.readString(kept));
        }
        assertFalse(Files.readString(file().toPath()).contains("not json"));
    }
}
