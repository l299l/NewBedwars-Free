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

/**
 * Reads and writes {@code data/groups.json}: {@code {"version":1,"revision":<time of last edit>,"groups":[...]}}
 * (the revision is used in network mode, where the newest set of groups wins).
 */
public final class ArenaGroupStore {
    public static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ArenaGroupStore() {}

    /** Groups and the revision of a file. */
    public record Loaded(List<ArenaGroup> groups, long revision) {}

    public static String toJson(List<ArenaGroup> groups) {
        return toJson(groups, 0);
    }

    public static String toJson(List<ArenaGroup> groups, long revision) {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        if (revision > 0) root.addProperty("revision", revision);
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

    /** The revision of a file's JSON, 0 when it has none. */
    public static long revisionOf(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.has("revision") ? root.get("revision").getAsLong() : 0;
    }

    /** Missing file = no groups. */
    public static List<ArenaGroup> load(Path file) throws IOException {
        return read(file).groups();
    }

    /** Missing file = no groups, revision 0. */
    public static Loaded read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return new Loaded(new ArrayList<>(), 0);
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return new Loaded(fromJson(json), revisionOf(json));
    }

    public static void save(Path file, List<ArenaGroup> groups, Path backupDir, int keep) throws IOException {
        save(file, groups, 0, backupDir, keep);
    }

    public static void save(Path file, List<ArenaGroup> groups, long revision, Path backupDir, int keep) throws IOException {
        AtomicFileWriter.write(file, toJson(groups, revision), backupDir, keep);
    }
}
