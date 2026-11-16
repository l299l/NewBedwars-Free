package com.l299l.newbedwars.panel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelI18nKeysTest {
    private static Set<String> keys(String language) throws IOException {
        Path path = Path.of("src/main/resources/web/i18n/" + language + ".json");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return new LinkedHashSet<>(json.keySet());
        }
    }

    @Test
    void polishHasEveryEnglishKey() throws IOException {
        Set<String> pl = keys("pl");
        List<String> missing = new ArrayList<>();
        for (String key : keys("en")) {
            if (!pl.contains(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "pl.json is missing " + missing.size() + " keys: " + missing);
    }
}
