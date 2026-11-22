package com.l299l.newbedwars.arena.maintenance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaTransferTest {
    @Test
    void oversizedArenaJsonIsRejectedBeforeReading(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve(ArenaTransfer.ARENA_ENTRY), new byte[(int) ArenaTransfer.MAX_JSON_BYTES + 1]);
        IOException e = assertThrows(IOException.class, () -> ArenaTransfer.readImport(dir));
        assertTrue(e.getMessage().contains("8 MB"));
    }

    @Test
    void missingArenaJsonIsRejected(@TempDir Path dir) {
        assertThrows(IOException.class, () -> ArenaTransfer.readImport(dir));
    }

    private static void write(Path dir, String teams) throws IOException {
        Files.writeString(dir.resolve(ArenaTransfer.ARENA_ENTRY),
                "{\"arenaName\":\"Arena\",\"worldName\":\"World\",\"teams\":" + teams + "}");
    }

    @Test
    void teamNamesWithIllegalCharactersAreRejected(@TempDir Path dir) throws Exception {
        write(dir, "[{\"name\":\"Red\"},{\"name\":\"x; op me\"}]");
        IOException e = assertThrows(IOException.class, () -> ArenaTransfer.readImport(dir));
        assertTrue(e.getMessage().contains("team name"));
    }

    @Test
    void teamWithoutAUsableNameIsRejected(@TempDir Path dir) throws Exception {
        write(dir, "[{\"color\":\"RED\"}]");
        assertThrows(IOException.class, () -> ArenaTransfer.readImport(dir));
        write(dir, "[{\"name\":\"{team}\"}]");
        assertThrows(IOException.class, () -> ArenaTransfer.readImport(dir));
    }

    @Test
    void safeTeamNamesPassTheNameCheckAndStopAtTheMissingMap(@TempDir Path dir) throws Exception {
        write(dir, "[{\"name\":\"Red\"},{\"name\":\"Team_2-b\"},{\"name\":\"Żółci\"},{\"name\":\"Team.1\"}]");
        IOException e = assertThrows(IOException.class, () -> ArenaTransfer.readImport(dir));
        assertTrue(e.getMessage().contains("no map"));
    }
}
