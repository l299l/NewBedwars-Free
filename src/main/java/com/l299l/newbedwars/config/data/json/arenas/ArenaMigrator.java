package com.l299l.newbedwars.config.data.json.arenas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Upgrades arena JSON files to the current schema, one version step at a time. */
public final class ArenaMigrator {
    public static final int CURRENT_VERSION = 2;

    private ArenaMigrator() {}

    /** Files written before versioning existed have no {@code schemaVersion} and count as version 1. */
    public static int versionOf(JsonObject json) {
        JsonElement v = json.get("schemaVersion");
        return v != null && v.isJsonPrimitive() ? v.getAsInt() : 1;
    }

    /** Migrates {@code json} in place to {@link #CURRENT_VERSION}. */
    public static JsonObject migrate(JsonObject json) {
        int version = versionOf(json);
        if (version > CURRENT_VERSION) {
            throw new IllegalStateException("Arena file has schema version " + version
                    + " but this plugin only understands up to " + CURRENT_VERSION + " - update NewBedwars");
        }
        if (version < 2) migrate1To2(json);
        return json;
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
