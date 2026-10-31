package com.l299l.newbedwars.network;

import com.l299l.newbedwars.api.view.NetworkMode;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.group.ArenaGroupStore;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.config.data.IOExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class NetworkGroupSyncTest {

    @AfterEach
    void tearDown() {
        new ArenaGroupManager();
    }

    private static ArenaGroup group(String name, String... arenas) {
        ArenaGroup g = new ArenaGroup(name, "MAP");
        for (String a : arenas) g.add(a);
        return g;
    }

    @Test
    void gameServerUsesOnlyItsOwnEntries() {
        List<ArenaGroup> local = NetworkGroupSync.forServer(List.of(
                group("solo", "castle", "game1:ruins", "game2:desert"),
                group("duo", "Game1:castle", "game1:lake")), "game1");
        assertEquals(List.of("castle", "ruins"), local.get(0).getArenas());
        // castle is already in solo: an arena stays in the first group that has it
        assertEquals(List.of("lake"), local.get(1).getArenas());
        assertEquals("MAP", local.get(0).getDisplayItem());
    }

    @Test
    void payloadRoundTrip() {
        NetworkGroups p = NetworkGroupSync.toPayload(List.of(group("solo", "castle", "game2:ruins")), 7L);
        assertEquals(7L, p.revision);
        List<ArenaGroup> back = NetworkGroupSync.fromPayload(p);
        assertEquals("solo", back.get(0).getName());
        assertEquals(List.of("castle", "game2:ruins"), back.get(0).getArenas());
        p.groups.add(new NetworkGroups.Group(" ", null, null));
        assertEquals(1, NetworkGroupSync.fromPayload(p).size());
    }

    @Test
    void gameServerTakesNewerGroupsAndCachesThem(@TempDir Path dir) throws Exception {
        ArenaGroupManager groups = new ArenaGroupManager();
        AtomicInteger applied = new AtomicInteger();
        NetworkGroupSync sync = new NetworkGroupSync(NetworkMode.GAME, "game1", mock(Messenger.class), dir,
                Logger.getAnonymousLogger(), applied::incrementAndGet);
        sync.start();
        assertTrue(groups.isNetworkManaged());
        assertTrue(groups.getGroups().isEmpty());

        sync.onGroups(NetworkGroupSync.toPayload(List.of(group("solo", "castle", "game2:ruins")), 5L));
        assertEquals(1, applied.get());
        assertEquals(List.of("castle"), groups.group("solo").getArenas());

        sync.onGroups(NetworkGroupSync.toPayload(List.of(), 4L)); // older: ignored
        sync.onGroups(NetworkGroupSync.toPayload(List.of(), 5L)); // same: ignored
        assertEquals(1, applied.get());
        assertNotNull(groups.group("solo"));

        IOExecutor.flush();
        ArenaGroupStore.Loaded cached = ArenaGroupStore.read(dir.resolve("data").resolve(NetworkGroupSync.CACHE_FILE));
        assertEquals(5L, cached.revision());
        assertEquals(List.of("castle", "game2:ruins"), cached.groups().get(0).getArenas());

        // after a restart the cache is used until the proxy sends something newer
        ArenaGroupManager again = new ArenaGroupManager();
        new NetworkGroupSync(NetworkMode.GAME, "game2", mock(Messenger.class), dir, Logger.getAnonymousLogger(), () -> {}).start();
        assertEquals(List.of("castle", "ruins"), again.group("solo").getArenas());
    }
}
