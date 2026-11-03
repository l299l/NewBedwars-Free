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
}
