package com.l299l.newbedwars.world.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MapTemplateRepairTest {

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(text.getBytes());
        }
        return out.toByteArray();
    }

    private static void write(Path file, byte[] data) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, data);
    }

    @Test
    void detectsTruncatedGzipOnly(@TempDir Path dir) throws IOException {
        byte[] full = gzip("world border data");
        write(dir.resolve("ok.dat"), full);
        write(dir.resolve("half.dat"), Arrays.copyOf(full, 10));
        write(dir.resolve("uid.dat"), new byte[]{1, 2, 3, 4});
        assertTrue(MapTemplateManager.isCompleteDat(dir.resolve("ok.dat")));
        assertFalse(MapTemplateManager.isCompleteDat(dir.resolve("half.dat")));
        assertTrue(MapTemplateManager.isCompleteDat(dir.resolve("uid.dat")), "not gzip = not checked");
    }

    @Test
    void recopiesAFileThatIsCompleteNow(@TempDir Path dir) throws IOException {
        byte[] full = gzip("level overrides");
        Path source = dir.resolve("world");
        Path copy = dir.resolve("copy");
        write(source.resolve("data/paper/level_overrides.dat"), full);
        write(copy.resolve("data/paper/level_overrides.dat"), Arrays.copyOf(full, 10));

        MapTemplateManager.repairIncompleteFiles(source, copy, null);

        assertArrayEquals(full, Files.readAllBytes(copy.resolve("data/paper/level_overrides.dat")));
    }

    @Test
    void fallsBackToThePreviousTemplate(@TempDir Path dir) throws IOException {
        byte[] full = gzip("world border");
        Path source = dir.resolve("world");
        Path copy = dir.resolve("copy");
        Path previous = dir.resolve("previous");
        write(source.resolve("data/minecraft/world_border.dat"), Arrays.copyOf(full, 10));
        write(copy.resolve("data/minecraft/world_border.dat"), Arrays.copyOf(full, 10));
        write(previous.resolve("data/minecraft/world_border.dat"), full);

        MapTemplateManager.repairIncompleteFiles(source, copy, previous);

        assertArrayEquals(full, Files.readAllBytes(copy.resolve("data/minecraft/world_border.dat")));
    }

    @Test
    void failsWhenNothingCompleteExists(@TempDir Path dir) throws IOException {
        byte[] half = Arrays.copyOf(gzip("x"), 10);
        Path source = dir.resolve("world");
        Path copy = dir.resolve("copy");
        write(source.resolve("a.dat"), half);
        write(copy.resolve("a.dat"), half);
        assertThrows(IOException.class, () -> MapTemplateManager.repairIncompleteFiles(source, copy, null));
    }
}
