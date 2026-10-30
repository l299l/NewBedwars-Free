package com.l299l.newbedwars.arena;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.config.data.json.arenas.ArenaMigrator;
import org.junit.jupiter.api.Test;

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
        when(arena.getSetupMode()).thenReturn(Setup.READY);
        when(arena.isEnabled()).thenReturn(true);
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
}
