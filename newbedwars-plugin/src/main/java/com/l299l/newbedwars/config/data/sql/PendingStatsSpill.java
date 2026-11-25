package com.l299l.newbedwars.config.data.sql;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.player.PlayerStats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

final class PendingStatsSpill {
    private final Path file;
    private final Logger log;

    PendingStatsSpill(Path file, Logger log) {
        this.file = file;
        this.log = log;
    }

    synchronized boolean append(UUID id, String name, PlayerStats d) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id.toString());
        o.addProperty("name", name);
        o.addProperty("wins", d.wins());
        o.addProperty("losses", d.losses());
        o.addProperty("kills", d.kills());
        o.addProperty("deaths", d.deaths());
        o.addProperty("finalKills", d.finalKills());
        o.addProperty("bedsBroken", d.bedsBroken());
        o.addProperty("gamesPlayed", d.gamesPlayed());
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, o + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.warning("[MySQL] The stats of " + name + " could not be saved and were kept in " + file.getFileName() + "; they are saved when the database is reachable again");
            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, "[MySQL] The stats of " + name + " could not be saved or kept in " + file, e);
            return false;
        }
    }

    synchronized boolean hasPending() {
        return Files.isRegularFile(file);
    }

    synchronized int replay(Connection c) throws SQLException {
        if (!Files.isRegularFile(file)) return 0;
        List<String> lines;
        try {
            lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.log(Level.WARNING, "[MySQL] Could not read " + file, e);
            return 0;
        }
        int done = 0;
        int bad = 0;
        SQLException failure = null;
        List<String> remaining = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            if (failure != null) {
                remaining.add(line);
                continue;
            }
            Entry e = parse(line);
            if (e == null) {
                bad++;
                remaining.add(line);
                continue;
            }
            try {
                PlayerSqlRepo.addStats(c, e.id, e.name, e.delta);
                done++;
            } catch (SQLException ex) {
                failure = ex;
                remaining.add(line);
            }
        }
        try {
            if (remaining.isEmpty()) {
                Files.deleteIfExists(file);
            } else if (done > 0) {
                AtomicFileWriter.write(file, String.join("\n", remaining) + "\n", null, 0);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "[MySQL] " + done + " kept stat update(s) were saved but " + file + " could not be updated; they may be saved twice on the next replay", e);
        }
        if (done > 0) log.info("[MySQL] Saved " + done + " kept stat update(s) from " + file.getFileName());
        if (bad > 0) log.warning("[MySQL] " + bad + " unreadable line(s) were left in " + file.getFileName());
        if (failure != null) throw failure;
        return done;
    }

    private record Entry(UUID id, String name, PlayerStats delta) {}

    private static Entry parse(String line) {
        try {
            JsonObject o = JsonParser.parseString(line).getAsJsonObject();
            return new Entry(UUID.fromString(o.get("id").getAsString()), o.get("name").getAsString(),
                    new PlayerStats(o.get("wins").getAsInt(), o.get("losses").getAsInt(), o.get("kills").getAsInt(),
                            o.get("deaths").getAsInt(), o.get("finalKills").getAsInt(), o.get("bedsBroken").getAsInt(),
                            o.get("gamesPlayed").getAsInt()));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
