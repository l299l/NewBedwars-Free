package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.NetworkJoinRequestEvent;
import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.view.NetworkArenaView;
import com.l299l.newbedwars.api.view.NetworkMode;
import com.l299l.newbedwars.api.view.PlayerStatsView;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;
import com.l299l.newbedwars.network.RemoteArena;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Network part of the API: actions outside a lobby, stats, view defaults and the join-request event. */
class ApiNetworkTest {
    private final NewBedwarsApiImpl api = new NewBedwarsApiImpl();

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void outsideNetworkModeActionsDoNothing() {
        Player p = mock(Player.class);
        assertEquals(NetworkMode.NONE, api.getNetworkMode());
        assertEquals(Optional.empty(), api.getServerId());
        assertFalse(api.requestNetworkJoin(p, "solo"));
        assertFalse(api.requestNetworkRejoin(p));
        assertEquals(Optional.empty(), api.getNetworkRejoin(p));
        assertTrue(api.getNetworkArenas().isEmpty());
        verifyNoInteractions(p);
    }

    @Test
    void statsComeFromThePlayerManager() {
        NewBedwars plugin = mock(NewBedwars.class);
        PlayerManager pm = new PlayerManager();
        pm.addStats("Leo", new PlayerStats(1, 2, 3, 4, 5, 6, 7));
        when(plugin.getPlayerManager()).thenReturn(pm);
        NewBedwars.plugin = plugin;
        PlayerStatsView s = api.getStats("Leo");
        assertEquals(1, s.getWins());
        assertEquals(2, s.getLosses());
        assertEquals(3, s.getKills());
        assertEquals(4, s.getDeaths());
        assertEquals(5, s.getFinalKills());
        assertEquals(6, s.getBedsBroken());
        assertEquals(7, s.getGamesPlayed());
        assertEquals(0, api.getStats("Nobody").getWins());
        assertEquals(0, api.getStats(null).getGamesPlayed());
    }

    @Test
    void networkArenaViewDefaults() {
        NetworkArenaView waiting = new RemoteArena("g1", new ArenaInfo("a", null, ArenaInfo.State.WAITING, 3, 8, 1, 8, true));
        NetworkArenaView full = new RemoteArena("g1", new ArenaInfo("b", null, ArenaInfo.State.STARTING, 9, 8, 1, 8, true));
        NetworkArenaView playing = new RemoteArena("g1", new ArenaInfo("c", null, ArenaInfo.State.PLAYING, 2, 8, 1, 8, true));
        NetworkArenaView off = new RemoteArena("g1", new ArenaInfo("d", null, ArenaInfo.State.DISABLED, 0, 8, 1, 8, true));
        assertTrue(waiting.isJoinable());
        assertEquals(5, waiting.getFreeSlots());
        assertTrue(full.isJoinable());
        assertEquals(0, full.getFreeSlots());
        assertFalse(playing.isJoinable());
        assertEquals(Optional.of(ArenaState.PLAYING), playing.getState());
        assertFalse(off.isJoinable());
    }

    @Test
    void joinRequestEventCarriesKindAndCanBeCancelled() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            PluginManager pm = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
            Player p = mock(Player.class);

            assertTrue(ApiEvents.networkJoinRequest(p, TargetKind.GROUP, "solo"));
            ArgumentCaptor<Event> e = ArgumentCaptor.forClass(Event.class);
            verify(pm).callEvent(e.capture());
            NetworkJoinRequestEvent ev = (NetworkJoinRequestEvent) e.getValue();
            assertSame(p, ev.getPlayer());
            assertEquals(NetworkJoinRequestEvent.Kind.GROUP, ev.getKind());
            assertEquals("solo", ev.getTarget());

            doAnswer(inv -> {
                if (inv.getArgument(0) instanceof Cancellable c) c.setCancelled(true);
                return null;
            }).when(pm).callEvent(any(Event.class));
            assertFalse(ApiEvents.networkJoinRequest(p, TargetKind.REJOIN, null));
            for (TargetKind k : TargetKind.values()) ApiEvents.networkJoinRequest(p, k, null); // every kind maps
        }
    }
}
