package com.l299l.newbedwars.config.data.json.arenas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ArenaMigrator {
    public static final int CURRENT_VERSION = 2;

    private ArenaMigrator() {}

    public static int versionOf(JsonObject json) {
        JsonElement v = json.get("schemaVersion");
        return v != null && v.isJsonPrimitive() ? v.getAsInt() : 1;
    }

    public static JsonObject migrate(JsonObject json) {
        int version = versionOf(json);
        if (version > CURRENT_VERSION) {
            throw new IllegalStateException("Arena file has schema version " + version
                    + " but this plugin only understands up to " + CURRENT_VERSION + " - update NewBedwars");
        }
        if (version < 2) migrate1To2(json);
        fillMissing(json);
        return json;
    }

    static void fillMissing(JsonObject json) {
        if (!isString(json, "arenaName")) throw new IllegalArgumentException("the arena file has no arenaName");
        if (!isString(json, "worldName")) json.addProperty("worldName", json.get("arenaName").getAsString());
        if (!json.has("enabled") || !json.get("enabled").isJsonPrimitive()) json.addProperty("enabled", false);
        if (!json.has("gamerules") || !json.get("gamerules").isJsonObject()) json.add("gamerules", new JsonObject());
        json.add("teams", keepObjectsWith(json.get("teams"), "name"));
        json.add("generators", keepObjectsWith(json.get("generators"), "type"));
    }

    private static boolean isString(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() && !json.get(key).getAsString().isEmpty();
    }

    private static JsonArray keepObjectsWith(JsonElement element, String key) {
        JsonArray out = new JsonArray();
        if (element == null || !element.isJsonArray()) return out;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonObject() && isString(entry.getAsJsonObject(), key)) out.add(entry);
        }
        return out;
    }

    private static void migrate1To2(JsonObject json) {
        if (!json.has("extra") || !json.get("extra").isJsonObject()) json.add("extra", new JsonObject());
        JsonElement teams = json.get("teams");
        if (teams != null && teams.isJsonArray()) {
            JsonArray array = teams.getAsJsonArray();
            for (JsonElement team : array) {
                if (team.isJsonObject() && (!team.getAsJsonObject().has("extra") || !team.getAsJsonObject().get("extra").isJsonObject())) {
                    team.getAsJsonObject().add("extra", new JsonObject());
                }
            }
        }
        json.addProperty("schemaVersion", 2);
    }
}
