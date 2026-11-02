package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.config.data.json.arenas.ArenaMigrator;
import com.l299l.newbedwars.world.template.MapTemplateManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

final class ArenaTransfer {
    static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_\\-]{1,64}");
    static final String ARENA_ENTRY = "arena.json";
    static final String TEMPLATE_ENTRY = "template";

    record Imported(String name, String worldName, JsonObject json, Path template) {}

    private ArenaTransfer() {}

    static boolean isSafeName(String name) {
        return name != null && SAFE_NAME.matcher(name).matches();
    }

    static Path resolveExport(Path exportsDir, String fileName) {
        Path zip = exportsDir.resolve(fileName).normalize();
        return zip.startsWith(exportsDir) && Files.isRegularFile(zip) ? zip : null;
    }

    static void stripWorldIdentity(Path template) throws IOException {
        Files.deleteIfExists(template.resolve("data").resolve("paper").resolve("metadata.dat"));
        Files.deleteIfExists(template.resolve("uid.dat"));
    }

    static Imported readImport(Path unpacked) throws IOException {
        Path jsonFile = unpacked.resolve(ARENA_ENTRY);
        if (!Files.isRegularFile(jsonFile)) throw new IOException("the zip has no " + ARENA_ENTRY);
        JsonObject json = ArenaMigrator.migrate(new Gson().fromJson(Files.readString(jsonFile, StandardCharsets.UTF_8), JsonObject.class));
        String name = json.get("arenaName").getAsString();
        String worldName = json.get("worldName").getAsString();
        if (!isSafeName(name) || !isSafeName(worldName)) {
            throw new IOException("the arena or world name contains illegal characters");
        }
        Path template = unpacked.resolve(TEMPLATE_ENTRY);
        if (!MapTemplateManager.isWorldFolder(template)) throw new IOException("the zip has no map");
        json.addProperty("enabled", false);
        json.addProperty("setup", "NO_SETUP");
        return new Imported(name, worldName, json, template);
    }
}
