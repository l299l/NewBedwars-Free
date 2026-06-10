package com.l299l.newbedwars.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l299l.newbedwars.arena.generators.Generator;
import org.bukkit.Location;

import java.io.IOException;

public class JsonUtils {
    public static String generatorToJson(Generator generator) {
        String sb = "{" +
                "\"type\": \"" + generator.getType() + "\"," +
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
                    "\"world\": \"" + location.getWorld().getName() + "\"" +
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
