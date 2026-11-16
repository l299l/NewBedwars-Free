package com.l299l.newbedwars.config.data.json.arenas;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l299l.newbedwars.config.data.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArenaMigratorTest {
    @Test
    void v1FileGetsSchemaVersionAndExtraObjects() {
        JsonObject json = JsonParser.parseString(
                "{\"arenaName\":\"a\",\"teams\":[{\"name\":\"red\"},{\"name\":\"blue\"}]}").getAsJsonObject();
        assertEquals(1, ArenaMigrator.versionOf(json));
        ArenaMigrator.migrate(json);
        assertEquals(ArenaMigrator.CURRENT_VERSION, ArenaMigrator.versionOf(json));
        assertTrue(json.get("extra").isJsonObject());
        assertTrue(json.getAsJsonArray("teams").get(1).getAsJsonObject().get("extra").isJsonObject());
    }

    @Test
    void existingExtraIsPreserved() {
        JsonObject json = JsonParser.parseString("{\"arenaName\":\"a\",\"extra\":{\"k\":\"v\"},\"teams\":[]}").getAsJsonObject();
        ArenaMigrator.migrate(json);
        assertEquals("v", json.getAsJsonObject("extra").get("k").getAsString());
    }

    @Test
    void newerSchemaIsRejected() {
        JsonObject json = JsonParser.parseString("{\"schemaVersion\":99}").getAsJsonObject();
        assertThrows(IllegalStateException.class, () -> ArenaMigrator.migrate(json));
    }

    @Test
    void missingFieldsGetSafeDefaults() {
        JsonObject json = JsonParser.parseString("{\"arenaName\":\"castle\",\"schemaVersion\":2}").getAsJsonObject();
        ArenaMigrator.migrate(json);
        assertEquals("castle", json.get("worldName").getAsString());
        assertFalse(json.get("enabled").getAsBoolean());
        assertTrue(json.get("gamerules").isJsonObject());
        assertEquals(0, json.getAsJsonArray("teams").size());
        assertEquals(0, json.getAsJsonArray("generators").size());
    }

    @Test
    void teamsWithoutNameAndGeneratorsWithoutTypeAreDropped() {
        JsonObject json = JsonParser.parseString("{\"arenaName\":\"a\",\"schemaVersion\":2,"
                + "\"teams\":[{\"name\":\"Red\"},{\"color\":\"BLUE\"},null],"
                + "\"generators\":[{\"type\":\"DIAMOND\"},{\"location\":{}}]}").getAsJsonObject();
        ArenaMigrator.migrate(json);
        assertEquals(1, json.getAsJsonArray("teams").size());
        assertEquals("Red", json.getAsJsonArray("teams").get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(1, json.getAsJsonArray("generators").size());
    }

    @Test
    void existingValuesAreKept() {
        JsonObject json = JsonParser.parseString("{\"arenaName\":\"a\",\"worldName\":\"w\",\"enabled\":true,"
                + "\"gamerules\":{\"x\":true},\"schemaVersion\":2}").getAsJsonObject();
        ArenaMigrator.migrate(json);
        assertEquals("w", json.get("worldName").getAsString());
        assertTrue(json.get("enabled").getAsBoolean());
        assertTrue(json.getAsJsonObject("gamerules").get("x").getAsBoolean());
    }

    @Test
    void fileWithoutArenaNameIsRejected() {
        JsonObject json = JsonParser.parseString("{\"schemaVersion\":2}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> ArenaMigrator.migrate(json));
    }

    @Test
    void quoteEscapesSpecialCharacters() {
        String quoted = JsonUtils.quote("we\"ird\\name\n");
        assertEquals("we\"ird\\name\n", JsonParser.parseString(quoted).getAsString());
        assertEquals("null", JsonUtils.quote(null));
    }
}
