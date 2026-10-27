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


}
