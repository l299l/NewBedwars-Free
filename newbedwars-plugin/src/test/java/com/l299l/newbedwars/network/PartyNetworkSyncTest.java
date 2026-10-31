package com.l299l.newbedwars.network;

import com.l299l.newbedwars.common.protocol.payload.PartySync;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** PartyManager as the read model of proxy-owned parties (network mode). */
class PartyNetworkSyncTest {
    static final UUID A = UUID.randomUUID();
    static final UUID B = UUID.randomUUID();
    static final UUID C = UUID.randomUUID();

    private static Player online(UUID id) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(id);
        return p;
    }

    @Test
    void syncCreatesUpdatesAndDisbands() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            PartyManager pm = new PartyManager(mock(Plugin.class), mock(Messages.class));
            pm.applyNetworkSync(new PartySync("p1", A, List.of(new PartySync.Member(A, "Alice"), new PartySync.Member(B, "Bob")), false, List.of()));
            Party party = pm.getParty(online(B));
            assertNotNull(party);
            assertTrue(party.isAdmin(A));
            assertEquals(List.of(A, B), party.getMembers());
            assertEquals("Alice", pm.nameOf(A), "name from the proxy when not online here");

            // B leaves, C joins, admin moves to C
            pm.applyNetworkSync(new PartySync("p1", C, List.of(new PartySync.Member(A, "Alice"), new PartySync.Member(C, "Cid")), false, List.of(B)));
            assertNull(pm.getParty(online(B)));
            assertSame(party, pm.getParty(online(C)));
            assertTrue(party.isAdmin(C));

            pm.applyNetworkSync(new PartySync("p1", C, List.of(), true, List.of(A, C)));
            assertNull(pm.getParty(online(A)));
            assertNull(pm.getParty(online(C)));
            assertEquals(0, party.size());
            assertEquals("?", pm.nameOf(UUID.randomUUID()));
            pm.applyNetworkSync(null);
        }
    }
}
