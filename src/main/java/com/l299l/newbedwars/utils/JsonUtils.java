package com.l299l.newbedwars.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonPrimitive;
import com.l299l.newbedwars.arena.generators.Generator;
import org.bukkit.Location;

import java.io.IOException;

public class JsonUtils {
    /** Returns {@code value} as a quoted, escaped JSON string ({@code null} for null). */
    public static String quote(String value) {
        return value == null ? "null" : new JsonPrimitive(value).toString();
    }

    public static String generatorToJson(Generator generator) {
        String sb = "{" +
                "\"type\": " + quote(String.valueOf(generator.getType())) + "," +
                "\"location\": " + locationToJson(generator.getLocation()) +
                "}";
        return sb;
    }

    public static String locationToJson(Location location) {
        try {
            String sb = "{" +
                    "\"x\": \"" + location.getX() + "\"," +
                    "\"y\": \"" + location.getY() + "\"," +
                    "\"z\": \"" + location.getZ() + "\"," +
                    "\"yaw\": \"" + location.getYaw() + "\"," +
                    "\"pitch\": \"" + location.getPitch() + "\"," +
                    "\"world\": " + quote(location.getWorld().getName()) +
                    "}";
            return sb;
        }catch (NullPointerException e) {
            return "null";
        }
    }

    public static String beautifyJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObject = mapper.readValue(json, Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }



    /** Replaces every {@code "world": oldWorld} (locations, at any depth) with {@code newWorld}. */
    public static void renameWorld(com.google.gson.JsonElement element, String oldWorld, String newWorld) {
        if (element == null) return;
        if (element.isJsonArray()) {
            for (com.google.gson.JsonElement e : element.getAsJsonArray()) renameWorld(e, oldWorld, newWorld);
        } else if (element.isJsonObject()) {
            com.google.gson.JsonObject obj = element.getAsJsonObject();
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : obj.entrySet()) {
                com.google.gson.JsonElement v = e.getValue();
                if (e.getKey().equals("world") && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()
                        && v.getAsString().equals(oldWorld)) {
                    e.setValue(new com.google.gson.JsonPrimitive(newWorld));
                } else {
                    renameWorld(v, oldWorld, newWorld);
                }
            }
        }
    }
}
