package com.l299l.newbedwars.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageKeysTest {
    private static Set<String> keys(String language) throws IOException {
        Path path = Path.of("src/main/resources/" + language + ".yml");
        Set<String> result = new LinkedHashSet<>();
        try (InputStream in = Files.newInputStream(path); Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Object root = new Yaml().load(reader);
            if (root instanceof Map) {
                flatten("", (Map<?, ?>) root, result);
            }
        }
        return result;
    }

    private static void flatten(String prefix, Map<?, ?> map, Set<String> out) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix + entry.getKey();
            if (entry.getValue() instanceof Map) {
                flatten(key + ".", (Map<?, ?>) entry.getValue(), out);
            } else {
                out.add(key);
            }
        }
    }

    private static List<String> missing(Set<String> reference, Set<String> other) {
        List<String> result = new ArrayList<>();
        for (String key : reference) {
            if (!other.contains(key)) {
                result.add(key);
            }
        }
        return result;
    }

    @Test
    void polishHasEveryEnglishKey() throws IOException {
        List<String> missing = missing(keys("en"), keys("pl"));
        assertTrue(missing.isEmpty(), "pl.yml is missing " + missing.size() + " keys: " + missing);
    }

    @Test
    void reportsKeysMissingInOtherLanguages() throws IOException {
        Set<String> english = keys("en");
        for (String language : new String[]{"de", "es", "fr", "ru"}) {
            List<String> missing = missing(english, keys(language));
            System.out.println(language + ".yml is missing " + missing.size() + " keys: " + missing);
        }
    }
}
