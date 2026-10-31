package com.l299l.newbedwars.config.data.json.arenas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AtomicFileWriterTest {
    @TempDir
    Path tmp;

    @Test
    void writesNewFileWithoutLeavingTempFile() throws IOException {
        Path target = tmp.resolve("arenas").resolve("a.json");
        AtomicFileWriter.write(target, "{\"v\":1}", null, 0);
        assertEquals("{\"v\":1}", Files.readString(target));
        assertFalse(Files.exists(tmp.resolve("arenas").resolve("a.json.tmp")));
    }

    @Test
    void overwriteBacksUpPreviousVersion() throws IOException {
        Path target = tmp.resolve("a.json");
        Path backups = tmp.resolve("bk");
        AtomicFileWriter.write(target, "one", backups, 5);
        AtomicFileWriter.write(target, "two", backups, 5);
        assertEquals("two", Files.readString(target));
        try (var s = Files.list(backups)) {
            var files = s.toList();
            assertEquals(1, files.size());
            assertEquals("one", Files.readString(files.get(0)));
        }
    }

    @Test
    void identicalContentCreatesNoBackup() throws IOException {
        Path target = tmp.resolve("a.json");
        Path backups = tmp.resolve("bk");
        AtomicFileWriter.write(target, "same", backups, 5);
        AtomicFileWriter.write(target, "same", backups, 5);
        assertFalse(Files.exists(backups));
    }

    @Test
    void backupsAreRotated() throws IOException, InterruptedException {
        Path target = tmp.resolve("a.json");
        Path backups = tmp.resolve("bk");
        for (int i = 0; i < 5; i++) {
            AtomicFileWriter.write(target, "v" + i, backups, 2);
            Thread.sleep(5);
        }
        try (var s = Files.list(backups)) {
            assertEquals(2, s.count());
        }
    }
}
