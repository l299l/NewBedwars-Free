package com.l299l.newbedwars.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageFillTest {
    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void addsMissingEnglishWhenBundledLacksIt() {
        Map<String, String> plan = LanguageFill.plan(map("A", "a", "B", "b"), map("A", "aa"), map("A", "aa"));
        assertEquals(map("B", "b"), plan);
    }

    @Test
    void replacesUntouchedEnglishWithBundledTranslation() {
        Map<String, String> plan = LanguageFill.plan(map("A", "a", "C", "c"), map("A", "aa", "C", "c"), map("A", "a", "C", "c"));
        assertEquals(map("A", "aa"), plan);
    }

    @Test
    void keepsUserValues() {
        Map<String, String> plan = LanguageFill.plan(map("A", "a"), map("A", "aa"), map("A", "mine"));
        assertTrue(plan.isEmpty());
    }

    @Test
    void ignoresCustomItemsNames() {
        Map<String, String> plan = LanguageFill.plan(map("CustomItemsNames.X", "x", "CustomItemsNames", "y"), map(), map());
        assertTrue(plan.isEmpty());
    }

    @Test
    void writeReplacesAndAppendsKeepingCommentsAndOthers() {
        String text = "# header\nA: \"a\"\n# note\nOther: \"keep\"\nLong: >-\n  wrapped\n  text\nZ: 1\n";
        String out = LanguageFill.write(text, map("A", "tr \"q\"", "Long", "new", "New", "x\ny"));
        assertEquals("# header\nA: \"tr \\\"q\\\"\"\n# note\nOther: \"keep\"\nLong: \"new\"\nZ: 1\nNew: \"x\\ny\"\n", out);
        assertFalse(out.contains("wrapped"));
    }

    @Test
    void keepsTheAdminsTranslationOfAKeyTheBundledLanguageLacks() {
        Map<String, String> plan = LanguageFill.plan(map("A", "a", "B", "b"), map("A", "aa"), map("A", "aa"), map("A", "aa", "B", "mine"));
        assertEquals(Map.of("B", "mine"), plan);
    }
}
