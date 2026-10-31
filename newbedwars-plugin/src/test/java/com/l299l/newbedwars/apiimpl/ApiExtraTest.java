package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.apiimpl.view.ApiExtra;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.view.ExtraData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ApiExtraTest {
    private JsonObject root;
    private AtomicInteger changes;
    private ExtraData mine;
    private ExtraData other;

    @BeforeEach
    void setUp() {
        root = new JsonObject();
        changes = new AtomicInteger();
        mine = new ApiExtra(() -> root, "MyAddon", changes::incrementAndGet);
        other = new ApiExtra(() -> root, "Other", changes::incrementAndGet);
    }

    @Test
    void valuesRoundTripWithTheirTypes() {
        mine.set("name", "Cloud");
        mine.set("count", 3);
        mine.set("ratio", 1.5);
        mine.set("on", true);
        assertEquals("Cloud", mine.getString("name", "?"));
        assertEquals(3, mine.getInt("count", 0));
        assertEquals(1.5, mine.getDouble("ratio", 0), 0.0001);
        assertTrue(mine.getBoolean("on", false));
    }

    @Test
    void missingKeysGiveTheDefault() {
        assertEquals("d", mine.getString("x", "d"));
        assertEquals(7, mine.getInt("x", 7));
        assertEquals(2.5, mine.getDouble("x", 2.5), 0.0001);
        assertTrue(mine.getBoolean("x", true));
        assertFalse(mine.has("x"));
        assertTrue(mine.keys().isEmpty());
    }

    @Test
    void wrongTypesGiveTheDefaultInsteadOfThrowing() {
        mine.set("text", "hello");
        mine.set("number", 4);
        assertEquals(9, mine.getInt("text", 9));
        assertFalse(mine.getBoolean("number", false));
        assertEquals(9.0, mine.getDouble("text", 9.0), 0.0001);
    }

    @Test
    void addonsDoNotSeeEachOther() {
        mine.set("k", "mine");
        other.set("k", "other");
        assertEquals("mine", mine.getString("k", ""));
        assertEquals("other", other.getString("k", ""));
        other.remove("k");
        assertEquals("mine", mine.getString("k", ""));
    }

    @Test
    void valuesLiveUnderTheAddonNameInsideTheExtraObject() {
        mine.set("k", 1);
        assertTrue(root.has("MyAddon"));
        assertEquals(1, root.getAsJsonObject("MyAddon").get("k").getAsInt());
    }

    @Test
    void removeDropsTheKeyAndAnEmptyNamespace() {
        mine.set("a", 1);
        mine.set("b", 2);
        mine.remove("a");
        assertEquals(Set.of("b"), mine.keys());
        mine.remove("b");
        assertFalse(root.has("MyAddon"));
        mine.remove("never"); // no-op
    }

    @Test
    void settingNullStringRemoves() {
        mine.set("a", "x");
        mine.set("a", (String) null);
        assertFalse(mine.has("a"));
    }

    @Test
    void everyWriteReportsAChange() {
        mine.set("a", 1);
        mine.set("b", "x");
        mine.remove("a");
        mine.remove("missing");
        assertEquals(3, changes.get());
    }

    @Test
    void existingDataFromAFileIsReadable() {
        JsonObject saved = new JsonObject();
        JsonObject ns = new JsonObject();
        ns.addProperty("lives", 5);
        saved.add("MyAddon", ns);
        root = saved;
        assertEquals(5, mine.getInt("lives", 0));
        assertEquals(Set.of("lives"), mine.keys());
    }

    @Test
    void anotherAddonsGarbageInTheNamespaceIsIgnored() {
        root.addProperty("MyAddon", "not an object");
        assertEquals("d", mine.getString("k", "d"));
        mine.set("k", "v");
        assertEquals("v", mine.getString("k", "d"));
    }
}
