package com.l299l.newbedwars.npc.fallback;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes {@code data/npcs.json}: {@code {"version":1,"npcs":[...]}}. */
public final class FallbackNpcStore {
    public static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private FallbackNpcStore() {}

    public static String toJson(List<FallbackNpc> npcs) {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.add("npcs", GSON.toJsonTree(npcs));
        return GSON.toJson(root);
    }

    public static List<FallbackNpc> fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("npcs")) return new ArrayList<>();
        List<FallbackNpc> npcs = GSON.fromJson(root.get("npcs"), new TypeToken<List<FallbackNpc>>() {}.getType());
        return npcs == null ? new ArrayList<>() : new ArrayList<>(npcs);
    }

    /** Missing file = no NPCs. */
    public static List<FallbackNpc> load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        return fromJson(Files.readString(file, StandardCharsets.UTF_8));
    }

    public static void save(Path file, List<FallbackNpc> npcs) throws IOException {
        AtomicFileWriter.write(file, toJson(npcs), null, 0);
    }
}
