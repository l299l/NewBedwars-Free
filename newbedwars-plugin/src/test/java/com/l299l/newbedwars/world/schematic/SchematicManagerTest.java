package com.l299l.newbedwars.world.schematic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SchematicManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void validNames() {
        assertTrue(SchematicManager.isValidName("castle"));
        assertTrue(SchematicManager.isValidName("Castle_2-big.v1"));
    }

    @Test
    void invalidNames() {
        assertFalse(SchematicManager.isValidName(null));
        assertFalse(SchematicManager.isValidName(""));
        assertFalse(SchematicManager.isValidName(".."));
        assertFalse(SchematicManager.isValidName("../secret"));
        assertFalse(SchematicManager.isValidName("a/b"));
        assertFalse(SchematicManager.isValidName("a\\b"));
        assertFalse(SchematicManager.isValidName("a..b"));
        assertFalse(SchematicManager.isValidName("a b"));
        assertFalse(SchematicManager.isValidName("x".repeat(65)));
    }

    @Test
    void findsSchemAndSchematicFilesInsideTheFolder() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("schematics"));
        Files.writeString(dir.resolve("one.schem"), "x");
        Files.writeString(dir.resolve("two.schematic"), "x");
        assertEquals("one.schem", SchematicManager.findIn(dir.toFile(), "one").getName());
        assertEquals("two.schematic", SchematicManager.findIn(dir.toFile(), "two").getName());
        assertNull(SchematicManager.findIn(dir.toFile(), "three"));
        assertNull(SchematicManager.findIn(null, "one"));
    }

    @Test
    void doesNotLeaveTheFolder() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("schematics"));
        Files.writeString(tempDir.resolve("secret.schem"), "x");
        assertNull(SchematicManager.findIn(dir.toFile(), "../secret"));
        assertNull(SchematicManager.findIn(new File(dir.toFile(), "../schematics"), "..\\secret"));
    }

    @Test
    void directoriesAreNotSchematics() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("schematics"));
        Files.createDirectory(dir.resolve("fake.schem"));
        assertNull(SchematicManager.findIn(dir.toFile(), "fake"));
    }
}
