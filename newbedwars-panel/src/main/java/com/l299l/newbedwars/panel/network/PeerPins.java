package com.l299l.newbedwars.panel.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PeerPins {
    private final Path file;
    private final Logger logger;
    private final Map<String, String> pins = new HashMap<>();
    private final Set<String> released = new HashSet<>();
    private final Path lockFile;
    private boolean damaged;

    public PeerPins(Path file, Logger logger) {
        this.file = file;
        this.lockFile = file.resolveSibling(file.getFileName() + ".locked");
        this.logger = logger;
        loadLock();
        load();
    }

    private void loadLock() {
        if (!Files.isRegularFile(lockFile)) return;
        damaged = true;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(lockFile, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject() || !parsed.getAsJsonObject().has("released")) return;
            for (JsonElement id : parsed.getAsJsonObject().getAsJsonArray("released")) {
                if (id.isJsonPrimitive()) released.add(id.getAsString());
            }
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not read " + lockFile + ", no peer is released from the lock", e);
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                damaged("it is not a JSON object");
                return;
            }
            boolean skipped = false;
            for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                String fingerprint = entry.getValue().isJsonPrimitive()
                        ? NetworkConfig.normalizeFingerprint(entry.getValue().getAsString()) : null;
                if (fingerprint == null || fingerprint.isEmpty()) skipped = true;
                else pins.put(entry.getKey(), fingerprint);
            }
            if (skipped) damaged("it has unreadable entries");
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not read " + file, e);
            damaged("it could not be read");
        }
    }

    private void damaged(String reason) {
        damaged = true;
        saveLock();
        Path target = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis());
        String moved;
        try {
            Files.move(file, target);
            moved = "Moved it to " + target.getFileName() + ".";
            save();
        } catch (IOException e) {
            moved = "It could not be moved aside (" + e.getMessage() + ").";
        }
        logger.severe("The panel peer pin file " + file.getFileName() + " is damaged (" + reason + "). " + moved
                + " Peers that were pinned in it are NOT trusted again automatically: verify each peer's certificate"
                + " fingerprint, then run /nbwpanel network forget <peer> to pin it again on its next contact. This stays in effect"
                + " after a restart until " + lockFile.getFileName() + " is deleted.");
    }

    public synchronized boolean firstUseAllowed(String id) {
        return !damaged || released.contains(id);
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
        if (damaged && released.add(id)) {
            saveLock();
            removed = true;
        }
        return removed;
    }

    private void save() {
        JsonObject o = new JsonObject();
        pins.forEach(o::addProperty);
        write(file, o.toString());
    }

    private void saveLock() {
        JsonArray ids = new JsonArray();
        released.forEach(ids::add);
        JsonObject o = new JsonObject();
        o.addProperty("locked", true);
        o.add("released", ids);
        write(lockFile, o.toString());
    }

    private void write(Path target, String content) {
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not write " + target, e);
        }
    }
}
