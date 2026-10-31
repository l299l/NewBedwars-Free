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
        JsonObject json = JsonParser.parseString("{\"extra\":{\"k\":\"v\"},\"teams\":[]}").getAsJsonObject();
        ArenaMigrator.migrate(json);
        assertEquals("v", json.getAsJsonObject("extra").get("k").getAsString());
    }

    @Test
    void newerSchemaIsRejected() {
        JsonObject json = JsonParser.parseString("{\"schemaVersion\":99}").getAsJsonObject();
        assertThrows(IllegalStateException.class, () -> ArenaMigrator.migrate(json));
    }

    @Test
    void quoteEscapesSpecialCharacters() {
        String quoted = JsonUtils.quote("we\"ird\\name\n");
        assertEquals("we\"ird\\name\n", JsonParser.parseString(quoted).getAsString());
        assertEquals("null", JsonUtils.quote(null));
    }
}
