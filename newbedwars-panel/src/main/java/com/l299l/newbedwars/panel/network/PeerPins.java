package com.l299l.newbedwars.panel.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PeerPins {
    private final Path file;
    private final Logger logger;
    private final Map<String, String> pins = new HashMap<>();

    public PeerPins(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return;
            for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) continue;
                String fingerprint = NetworkConfig.normalizeFingerprint(entry.getValue().getAsString());
                if (fingerprint != null && !fingerprint.isEmpty()) pins.put(entry.getKey(), fingerprint);
            }
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not read " + file, e);
        }
    }

    public synchronized String get(String id) {
        return pins.get(id);
    }

    public synchronized void put(String id, String fingerprint) {
        pins.put(id, fingerprint);
        save();
    }

    public synchronized boolean remove(String id) {
        boolean removed = pins.remove(id) != null;
        if (removed) save();
        return removed;
    }

    private void save() {
        JsonObject o = new JsonObject();
        pins.forEach(o::addProperty);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, o.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not write " + file, e);
        }
    }
}
