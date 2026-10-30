package com.l299l.newbedwars.npc.fallback;

import com.l299l.newbedwars.npc.JoinNpcAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FallbackNpcStoreTest {

    private static FallbackNpc npc(int id, String action, String target) {
        FallbackNpc n = new FallbackNpc();
        n.id = id;
        n.entityType = "VILLAGER";
        n.world = "lobby";
        n.x = 1.5;
        n.y = 64;
        n.z = -3.25;
        n.yaw = 90f;
        n.pitch = 10f;
        n.action = action;
        n.target = target;
        n.entity = UUID.randomUUID();
        return n;
    }

    @Test
    void roundTrip() {
        String json = FallbackNpcStore.toJson(List.of(npc(1, "GROUP", "solo_*"), npc(2, "SELECTOR", null)));
        assertTrue(json.contains("\"version\": 1"));
        assertFalse(json.contains("entity\""), "the spawned entity is not saved");

        List<FallbackNpc> back = FallbackNpcStore.fromJson(json);
        assertEquals(2, back.size());
        FallbackNpc a = back.get(0);
        assertEquals(1, a.id);
        assertEquals("VILLAGER", a.entityType);
        assertEquals("lobby", a.world);
        assertEquals(-3.25, a.z);
        assertEquals(90f, a.yaw);
        assertNull(a.entity);
        assertEquals(JoinNpcAction.parse("GROUP", "solo_*"), a.joinAction());
        assertEquals(JoinNpcAction.parse("SELECTOR", null), back.get(1).joinAction());
    }

    @Test
    void invalidActionIsKeptButNotUsable() {
        List<FallbackNpc> back = FallbackNpcStore.fromJson(FallbackNpcStore.toJson(List.of(npc(1, "FLY", "x"))));
        assertEquals(1, back.size());
        assertNull(back.get(0).joinAction());
    }

    @Test
    void saveAndLoadFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("data").resolve("npcs.json");
        assertTrue(FallbackNpcStore.load(file).isEmpty(), "missing file = no NPCs");
        FallbackNpcStore.save(file, List.of(npc(3, "ARENA", "castle")));
        assertTrue(Files.isRegularFile(file));
        List<FallbackNpc> back = FallbackNpcStore.load(file);
        assertEquals(3, back.get(0).id);
        assertEquals("castle", back.get(0).target);
    }

    @Test
    void emptyObjectHasNoNpcs() {
        assertTrue(FallbackNpcStore.fromJson("{\"version\":1}").isEmpty());
    }
}
