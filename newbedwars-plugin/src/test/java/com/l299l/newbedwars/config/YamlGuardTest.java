package com.l299l.newbedwars.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlGuardTest {
    private final Logger logger = Logger.getLogger("YamlGuardTest");

    @TempDir
    Path dir;

    @Test
    void validFileIsKept() throws Exception {
        Path file = dir.resolve("en.yml");
        Files.writeString(file, "Key: 'value'\nOther: 3\n", StandardCharsets.UTF_8);
        assertTrue(YamlGuard.ensureValid(file.toFile(), logger));
        assertTrue(Files.exists(file));
    }

    @Test
    void missingFileIsUsable() {
        assertTrue(YamlGuard.ensureValid(dir.resolve("none.yml").toFile(), logger));
        assertTrue(YamlGuard.ensureValid(null, logger));
    }

    @Test
    void invalidFileIsMovedAsideWithItsContent() throws Exception {
        Path file = dir.resolve("scoreboards.yml");
        String broken = "Lobby:\n  Title: 'My custom title\n  Lines: [a, b\n";
        Files.writeString(file, broken, StandardCharsets.UTF_8);
        File asFile = file.toFile();

        assertFalse(YamlGuard.ensureValid(asFile, logger));
        assertFalse(asFile.exists());
        try (Stream<Path> files = Files.list(dir)) {
            Path moved = files.filter(p -> p.getFileName().toString().startsWith("scoreboards.yml.corrupt-"))
                    .findFirst().orElseThrow();
            assertEquals(broken, Files.readString(moved, StandardCharsets.UTF_8));
        }
    }
}
