package com.l299l.newbedwars.arena.group;

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

/** Reads and writes {@code data/groups.json}: {@code {"version":1,"groups":[...]}}. */
public final class ArenaGroupStore {
    public static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ArenaGroupStore() {}

    public static String toJson(List<ArenaGroup> groups) {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.add("groups", GSON.toJsonTree(groups));
        return GSON.toJson(root);
    }

    public static List<ArenaGroup> fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("groups")) return new ArrayList<>();
        List<ArenaGroup> groups = GSON.fromJson(root.get("groups"), new TypeToken<List<ArenaGroup>>() {}.getType());
        List<ArenaGroup> result = new ArrayList<>();
        if (groups != null) {
            for (ArenaGroup g : groups) {
                if (g != null && g.getName() != null && !g.getName().isBlank()) result.add(g);
            }
        }
        return result;
    }

    /** Missing file = no groups. */
    public static List<ArenaGroup> load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return new ArrayList<>();
        return fromJson(Files.readString(file, StandardCharsets.UTF_8));
    }

    public static void save(Path file, List<ArenaGroup> groups, Path backupDir, int keep) throws IOException {
        AtomicFileWriter.write(file, toJson(groups), backupDir, keep);
    }
}
