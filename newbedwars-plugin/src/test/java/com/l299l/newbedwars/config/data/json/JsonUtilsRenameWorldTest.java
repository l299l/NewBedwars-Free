package com.l299l.newbedwars.config.data.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilsRenameWorldTest {

    @Test
    void renamesEveryLocationWorldAtAnyDepth() {
        JsonObject arena = JsonParser.parseString("""
                {"worldName":"Castle","waitingSpawn":{"world":"Castle","x":1},
                 "teams":[{"spawn":{"world":"Castle"},"bed":{"world":"Other"}}],
                 "extra":{"note":"Castle"}}""").getAsJsonObject();
        JsonUtils.renameWorld(arena, "Castle", "Castle2");
        assertEquals("Castle2", arena.getAsJsonObject("waitingSpawn").get("world").getAsString());
        JsonObject team = arena.getAsJsonArray("teams").get(0).getAsJsonObject();
        assertEquals("Castle2", team.getAsJsonObject("spawn").get("world").getAsString());
        assertEquals("Other", team.getAsJsonObject("bed").get("world").getAsString(), "other worlds stay");
        assertEquals("Castle", arena.getAsJsonObject("extra").get("note").getAsString(), "only world keys change");
        assertEquals("Castle", arena.get("worldName").getAsString(), "worldName is set by the caller");
    }
}
