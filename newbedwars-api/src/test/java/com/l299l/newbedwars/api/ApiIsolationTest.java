package com.l299l.newbedwars.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The api package is the contract for addons: it must not depend on plugin internals. */
class ApiIsolationTest {

    @Test
    void apiPackageImportsNothingFromInternals() throws IOException {
        Path root = Path.of("src/main/java/com/l299l/newbedwars/api");
        assertTrue(Files.isDirectory(root), "api source folder not found");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    String t = line.trim();
                    if (t.startsWith("import com.l299l.newbedwars.")) {
                        assertTrue(t.startsWith("import com.l299l.newbedwars.api."), file + " imports internals: " + t);
                    }
                    assertFalse(t.contains("com.l299l.newbedwars.arena") || t.contains("com.l299l.newbedwars.apiimpl"),
                            file + " references internals: " + t);
                }
            }
        }
    }
}
