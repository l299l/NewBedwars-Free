package com.l299l.newbedwars.arena;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.bossbar.BossBarManager;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.config.data.json.arenas.ArenaMigrator;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArenaSerializerTest {

    private Arena arena() {
        Arena arena = mock(Arena.class);
        when(arena.getArenaName()).thenReturn("Castle \"1\"");
        when(arena.getWorldName()).thenReturn("castle");
        when(arena.getMinPlayers()).thenReturn(2);
        when(arena.getMaxInTeam()).thenReturn(4);
        when(arena.getWaitingTime()).thenReturn(30);
        when(arena.getRespawnTime()).thenReturn(5);
        when(arena.setupToSave()).thenReturn(Setup.READY);
        when(arena.enabledToSave()).thenReturn(true);
        when(arena.getQuickVoidY()).thenReturn(null);
        when(arena.getGamerules()).thenReturn(new Gamerules());
        when(arena.getTeams()).thenReturn(Map.of());
        when(arena.getGenerators()).thenReturn(List.of());
        JsonObject extra = new JsonObject();
        extra.addProperty("addon", 1);
        when(arena.getExtra()).thenReturn(extra);
        JsonObject unknown = new JsonObject();
        unknown.addProperty("future", "kept");
        when(arena.getUnknownFields()).thenReturn(unknown);
        return arena;
    }

    @Test
    void writesValidJsonWithAllTopLevelFields() {
        Arena arena = arena();
        when(arena.getUnresolvedGamePhasesId()).thenReturn("MissingPhases");
        JsonObject json = JsonParser.parseString(ArenaSerializer.toJson(arena)).getAsJsonObject();

        assertEquals(ArenaMigrator.CURRENT_VERSION, json.get("schemaVersion").getAsInt());
        assertEquals("Castle \"1\"", json.get("arenaName").getAsString());
        assertEquals("castle", json.get("worldName").getAsString());
        assertEquals(5, json.get("respawnTime").getAsInt());
        assertEquals("READY", json.get("setup").getAsString());
        assertTrue(json.get("enabled").getAsBoolean());
        assertTrue(json.get("waitingSpawn").isJsonNull());
        assertTrue(json.get("quickVoidY").isJsonNull());
        assertEquals("MissingPhases", json.get("gamePhasesId").getAsString());
        assertEquals("null", json.get("generatorsLeveling").getAsString());
        assertEquals(0, json.getAsJsonArray("teams").size());
        assertEquals(1, json.getAsJsonObject("extra").get("addon").getAsInt());
        assertEquals("kept", json.get("future").getAsString());
    }

    private static JsonObject saved(Arena arena) {
        return JsonParser.parseString(ArenaSerializer.toJson(arena)).getAsJsonObject();
    }

    private static void assertSaved(boolean enabled, Setup setup, Arena arena) {
        JsonObject json = saved(arena);
        assertEquals(enabled, json.get("enabled").getAsBoolean());
        assertEquals(setup.name(), json.get("setup").getAsString());
    }

    @Test
    void autoDisableKeepsTheSavedStateUntilAnAdminActs() {
        NewBedwars oldPlugin = NewBedwars.plugin;
        Integer oldMin = Properties.DefaultMinPlayers;
        Integer oldMaxInTeam = Properties.DefaultMaxInTeam;
        Integer oldWaiting = Properties.DefaultWaitingTime;
        Integer oldRespawn = Properties.DefaultRespawnTime;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(mock(Server.class));
            Properties.DefaultMinPlayers = 2;
            Properties.DefaultMaxInTeam = 1;
            Properties.DefaultWaitingTime = 10;
            Properties.DefaultRespawnTime = 5;
            NewBedwars plugin = mock(NewBedwars.class);
            when(plugin.getGeneratorLeveling()).thenReturn(new HashMap<>());
            when(plugin.getScoreboardManager()).thenReturn(mock(ScoreboardManager.class));
            when(plugin.getBossBarManager()).thenReturn(mock(BossBarManager.class));
            NewBedwars.plugin = plugin;
            World world = mock(World.class);
            when(world.getName()).thenReturn("castle");
            Arena arena = new Arena("Castle", world);

            arena.enable();
            arena.autoDisable();
            assertFalse(arena.isEnabled());
            assertEquals(Setup.NO_SETUP, arena.getSetupMode());
            assertTrue(arena.isAutoDisabled());
            assertSaved(true, Setup.READY, arena);
            arena.disableBroken("still broken");
            assertSaved(true, Setup.READY, arena);
            arena.changeSetup(Setup.NORMAL_SETUP);
            assertFalse(arena.isAutoDisabled());
            assertSaved(false, Setup.NORMAL_SETUP, arena);

            arena.enable();
            arena.disableBroken("reset failed");
            assertTrue(arena.isAutoDisabled());
            assertSaved(true, Setup.READY, arena);
            arena.enable();
            assertFalse(arena.isAutoDisabled());
            assertSaved(true, Setup.READY, arena);
            arena.disableBroken("reset failed again");
            arena.setStatus(GameStatus.waiting);
            arena.disable();
            assertFalse(arena.isAutoDisabled());
            assertSaved(false, Setup.NO_SETUP, arena);
        } finally {
            NewBedwars.plugin = oldPlugin;
            Properties.DefaultMinPlayers = oldMin;
            Properties.DefaultMaxInTeam = oldMaxInTeam;
            Properties.DefaultWaitingTime = oldWaiting;
            Properties.DefaultRespawnTime = oldRespawn;
            ArenaRegistry.clear();
        }
    }
}
