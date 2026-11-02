package com.l299l.newbedwars.config.data.yaml;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SafeYamlTest {

    @Test
    void javaTagsAreRejected() {
        Yaml yaml = SafeYaml.create();
        assertThrows(YAMLException.class, () -> yaml.load("a: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL [\"http://localhost/x\"]]]]"));
        assertThrows(YAMLException.class, () -> yaml.load("!!java.lang.ProcessBuilder [[\"id\"]]"));
    }

    @Test
    void plainDataLoadsAsBefore() {
        Map<String, Object> map = SafeYaml.create().load("Size: 54\nCloseOnTransaction: true\nSlots:\n  1: wool\nItems:\n- a\n- b\n");
        assertEquals(54, map.get("Size"));
        assertEquals(Boolean.TRUE, map.get("CloseOnTransaction"));
        assertEquals("wool", ((Map<?, ?>) map.get("Slots")).get(1));
        assertEquals(List.of("a", "b"), map.get("Items"));
    }

    @Test
    void dumpUsesBlockStyle() {
        String out = SafeYaml.create().dump(Map.of("k", List.of("a", "b")));
        assertEquals("k:\n- a\n- b\n", out);
    }
}
