package com.l299l.newbedwars.world.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapTemplateManagerTest {
    @TempDir
    Path tmp;
    Path maps;
    Path world;

    @BeforeEach
    void setUp() throws IOException {
        maps = tmp.resolve("maps");
        world = tmp.resolve("world");
        Files.createDirectories(world.resolve("region"));
        Files.createDirectories(world.resolve("playerdata"));
        Files.writeString(world.resolve("level.dat"), "level");
        Files.writeString(world.resolve("session.lock"), "lock");
        Files.writeString(world.resolve("region").resolve("r.0.0.mca"), "blocks");
        Files.writeString(world.resolve("playerdata").resolve("p.dat"), "player");
    }

    @Test
    void createTemplateCopiesWorldWithoutExcludes() throws IOException {
        MapTemplateManager.createTemplate(maps, "castle", world, List.of("session.lock", "playerdata"));
        Path t = MapTemplateManager.templateDir(maps, "castle");
        assertTrue(MapTemplateManager.hasTemplate(maps, "castle"));
        assertTrue(Files.exists(t.resolve("region").resolve("r.0.0.mca")));
        assertFalse(Files.exists(t.resolve("session.lock")));
        assertFalse(Files.exists(t.resolve("playerdata")));
        assertTrue(Files.exists(maps.resolve("castle").resolve(MapTemplateManager.META_FILE)));
    }

    @Test
    void createTemplateFailsWithoutLevelDatAndKeepsOldTemplate() throws IOException {
        MapTemplateManager.createTemplate(maps, "castle", world, List.of());
        Path empty = Files.createDirectories(tmp.resolve("empty"));
        assertThrows(IOException.class, () -> MapTemplateManager.createTemplate(maps, "castle", empty, List.of()));
        assertTrue(MapTemplateManager.hasTemplate(maps, "castle"));
    }

    @Test
    void restoreReplacesDamagedWorld() throws IOException {
        MapTemplateManager.createTemplate(maps, "castle", world, List.of("session.lock"));
        Files.writeString(world.resolve("region").resolve("r.0.0.mca"), "DAMAGED", StandardCharsets.UTF_8);
        Files.writeString(world.resolve("extra.txt"), "x");
        MapTemplateManager.restoreTemplate(maps, "castle", world, List.of());
        assertEquals("blocks", Files.readString(world.resolve("region").resolve("r.0.0.mca")));
        assertFalse(Files.exists(world.resolve("extra.txt")));
    }

    @Test
    void restoreWithoutTemplateFailsAndKeepsWorld() {
        assertThrows(IOException.class, () -> MapTemplateManager.restoreTemplate(maps, "none", world, List.of()));
        assertTrue(Files.exists(world.resolve("level.dat")));
    }

    @Test
    void dirtyMarkerLifecycle() throws IOException {
        assertFalse(MapTemplateManager.isDirty(maps, "castle"));
        MapTemplateManager.markDirty(maps, "castle");
        assertTrue(MapTemplateManager.isDirty(maps, "castle"));
        MapTemplateManager.clearDirty(maps, "castle");
        assertFalse(MapTemplateManager.isDirty(maps, "castle"));
    }

    @Test
    void zipRoundTrip() throws IOException {
        MapTemplateManager.createTemplate(maps, "castle", world, List.of("session.lock"));
        Path zip = tmp.resolve("b").resolve("one.zip");
        assertTrue(MapTemplateManager.zipTemplate(maps, "castle", zip));
        MapTemplateManager.deleteTree(MapTemplateManager.templateDir(maps, "castle"));
        assertFalse(MapTemplateManager.hasTemplate(maps, "castle"));
        MapTemplateManager.restoreTemplateFromZip(maps, "castle", zip);
        assertTrue(MapTemplateManager.hasTemplate(maps, "castle"));
    }

    @Test
    void unzipRejectsZipSlip() throws IOException {
        Path zip = tmp.resolve("evil.zip");
        try (var out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new java.util.zip.ZipEntry("../evil.txt"));
            out.write(1);
            out.closeEntry();
        }
        assertThrows(IOException.class, () -> MapTemplateManager.unzip(zip, tmp.resolve("target")));
        assertFalse(Files.exists(tmp.resolve("evil.txt")));
    }

    @Test
    void rotateKeepsNewest() throws IOException {
        Path dir = Files.createDirectories(tmp.resolve("bk"));
        for (String n : List.of("20260101", "20260102", "20260103")) Files.writeString(dir.resolve(n + ".zip"), n);
        MapTemplateManager.rotate(dir, ".zip", 2);
        assertFalse(Files.exists(dir.resolve("20260101.zip")));
        assertTrue(Files.exists(dir.resolve("20260103.zip")));
    }
}
