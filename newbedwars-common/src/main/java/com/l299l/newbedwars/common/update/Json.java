package com.l299l.newbedwars.common.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

final class Json {
    private Json() {}

    static JsonArray array(String json, String source) throws IOException {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonArray()) return root.getAsJsonArray();
        } catch (RuntimeException ignored) {
        }
        throw new IOException("Unreadable answer from " + source);
    }

    static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    static boolean bool(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
    }

    static boolean contains(JsonElement array, String value) {
        if (array == null || !array.isJsonArray()) return false;
        for (JsonElement element : array.getAsJsonArray()) {
            if (element.isJsonPrimitive() && value.equalsIgnoreCase(element.getAsString())) return true;
        }
        return false;
    }
}
