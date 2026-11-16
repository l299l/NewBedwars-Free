package com.l299l.newbedwars.player;

import com.l299l.newbedwars.config.Language;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerManagerTest {

    private static PlayerProfile ins(String name) {
        return new PlayerProfile(UUID.randomUUID(), name, Language.English, PlayerProfile.defaultFastBuy());
    }

    @Test
    void addAndGetPlayerRoundTrip() {
        PlayerManager pm = new PlayerManager();
        PlayerProfile p = ins("Alice");
        pm.addPlayer(p);
        assertSame(p, pm.getPlayer("Alice"));
    }

    @Test
    void getPlayerReturnsNullForUnknownName() {
        PlayerManager pm = new PlayerManager();
        assertNull(pm.getPlayer("nobody"));
    }

    @Test
    void removePlayerDeletesEntry() {
        PlayerManager pm = new PlayerManager();
        pm.addPlayer(ins("Alice"));
        pm.removePlayer("Alice");
        assertNull(pm.getPlayer("Alice"));
    }

    @Test
    void removePlayerForUnknownNameIsNoOp() {
        PlayerManager pm = new PlayerManager();
        assertDoesNotThrow(() -> pm.removePlayer("nobody"));
    }

    @Test
    void updatePlayerLanguageUpdatesExistingEntry() {
        PlayerManager pm = new PlayerManager();
        pm.addPlayer(new PlayerProfile(UUID.randomUUID(), "Alice", Language.English, PlayerProfile.defaultFastBuy()));
        pm.updatePlayerLanguage("Alice", Language.Polish);
        assertEquals(Language.Polish, pm.getPlayer("Alice").language());
    }

    @Test
    void getPlayersReturnsLiveMap() {
        PlayerManager pm = new PlayerManager();
        assertTrue(pm.getPlayers().isEmpty());
        pm.addPlayer(ins("Alice"));
        assertEquals(1, pm.getPlayers().size());
    }

    @Test
    void unchangedFastBuyIsNotSaved() {
        com.l299l.newbedwars.NewBedwars saved = com.l299l.newbedwars.NewBedwars.plugin;
        com.l299l.newbedwars.NewBedwars plugin = org.mockito.Mockito.mock(com.l299l.newbedwars.NewBedwars.class);
        com.l299l.newbedwars.config.data.DataManager data = org.mockito.Mockito.mock(com.l299l.newbedwars.config.data.DataManager.class);
        org.mockito.Mockito.when(plugin.getDataManager()).thenReturn(data);
        com.l299l.newbedwars.NewBedwars.plugin = plugin;
        try {
            PlayerManager pm = new PlayerManager();
            pm.addPlayer(new PlayerProfile(UUID.randomUUID(), "Alice", Language.English,
                    new java.util.HashMap<>(java.util.Map.of("blocks", java.util.List.of("wool")))));
            pm.updateFastBuy("Alice", new java.util.HashMap<>(java.util.Map.of("blocks", new java.util.ArrayList<>(java.util.List.of("wool")))));
            org.mockito.Mockito.verifyNoInteractions(data);
            pm.updateFastBuy("Alice", new java.util.HashMap<>(java.util.Map.of("blocks", java.util.List.of("stone"))));
            org.mockito.Mockito.verify(data).savePlayerData();
        } finally {
            com.l299l.newbedwars.NewBedwars.plugin = saved;
        }
    }

    @Test
    void renamedPlayerKeepsProfileAndStats() {
        PlayerManager pm = new PlayerManager();
        UUID id = UUID.randomUUID();
        pm.addPlayer(new PlayerProfile(id, "OldName", Language.Polish, PlayerProfile.defaultFastBuy()));
        pm.addStats(id, "OldName", new PlayerStats(3, 0, 0, 0, 0, 0, 3));
        pm.identify(id, "NewName");
        assertEquals(3, pm.getStats("NewName").wins());
        assertEquals(Language.Polish, pm.getPlayer("newname").language());
        assertEquals("NewName", pm.getPlayer(id).name());
        assertNull(pm.getPlayer("OldName"));
        assertEquals(0, pm.getStats("OldName").wins());
        assertEquals("NewName", pm.getTopByWins(1).get(0).getKey());
    }

    @Test
    void nameTakenOverByAnotherPlayerPointsToTheNewOwner() {
        PlayerManager pm = new PlayerManager();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        pm.addStats(first, "Steve", new PlayerStats(9, 0, 0, 0, 0, 0, 9));
        pm.identify(second, "Steve");
        assertEquals(0, pm.getStats("Steve").wins());
        assertEquals(9, pm.getStats(first).wins());
    }

    @Test
    void mergeAddsStatsOfTheSameUuid() {
        PlayerManager pm = new PlayerManager();
        UUID id = UUID.randomUUID();
        pm.mergeStats(id, "A", new PlayerStats(1, 1, 1, 1, 1, 1, 2));
        pm.mergeStats(id, "B", new PlayerStats(1, 0, 2, 0, 0, 0, 1));
        assertEquals(new PlayerStats(2, 1, 3, 1, 1, 1, 3), pm.getStats(id));
    }
}
