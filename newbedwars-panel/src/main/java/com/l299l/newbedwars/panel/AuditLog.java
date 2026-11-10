package com.l299l.newbedwars.panel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AuditLog {
    public static final int KEEP_IN_MEMORY = 500;
    private static final long ROTATE_BYTES = 5L * 1024 * 1024;

    public record Entry(long time, String user, String ip, String action, String target, String result) {}

    private final Path file;
    private final Logger logger;
    private final LongSupplier clock;
    private final Deque<Entry> recent = new ArrayDeque<>();

    public AuditLog(Path file, Logger logger, LongSupplier clock) {
        this.file = file;
        this.logger = logger;
        this.clock = clock;
        try {
            if (Files.isRegularFile(file) && Files.size(file) > ROTATE_BYTES) {
                Files.move(file, file.resolveSibling(file.getFileName() + ".1"), StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.isRegularFile(file)) load();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not read " + file, e);
        }
    }

    private void load() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JsonObject o = JsonParser.parseString(line).getAsJsonObject();
                    remember(new Entry(o.get("time").getAsLong(), text(o, "user"), text(o, "ip"),
                            text(o, "action"), text(o, "target"), text(o, "result")));
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    private static String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static String toJson(Entry entry) {
        JsonObject o = new JsonObject();
        o.addProperty("time", entry.time());
        o.addProperty("user", entry.user());
        o.addProperty("ip", entry.ip());
        o.addProperty("action", entry.action());
        o.addProperty("target", entry.target());
        o.addProperty("result", entry.result());
        return o.toString();
    }

    private void remember(Entry entry) {
        recent.addLast(entry);
        while (recent.size() > KEEP_IN_MEMORY) recent.removeFirst();
    }

    public synchronized void record(String user, String ip, String action, String target, String result) {
        Entry entry = new Entry(clock.getAsLong(), user, ip, action, target, result);
        remember(entry);
        logger.info("[Audit] " + (user == null ? "-" : user) + " (" + ip + ") " + action
                + (target == null ? "" : " " + target) + " -> " + result);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                out.write(toJson(entry));
                out.newLine();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not write " + file, e);
        }
    }

    public synchronized List<Entry> recent(int limit) {
        List<Entry> result = new ArrayList<>();
        for (Iterator<Entry> it = recent.descendingIterator(); it.hasNext() && result.size() < limit; ) result.add(it.next());
        return result;
    }
}
