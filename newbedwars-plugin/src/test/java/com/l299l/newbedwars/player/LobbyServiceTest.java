package com.l299l.newbedwars.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LobbyServiceTest {
    private MockedStatic<Bukkit> bukkit;
    private PluginManager pm;
    private Player player;
    private Location lobby;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        pm = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
        NewBedwars.plugin = mock(NewBedwars.class);
        lobby = mock(Location.class);
        when(NewBedwars.plugin.getLobbyLocation()).thenReturn(lobby);
        player = mock(Player.class);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
        bukkit.close();
    }

    @Test
    void teleportsToLobbyWhenNobodyIntervenes() {
        assertTrue(LobbyService.sendToLobby(player, PlayerSendToLobbyEvent.Reason.LEFT_ARENA));
        verify(player).teleport(lobby);
    }

    @Test
    void cancelledEventLeavesPlayerToTheListener() {
        doAnswer(inv -> {
            ((PlayerSendToLobbyEvent) inv.getArgument(0)).setCancelled(true);
            return null;
        }).when(pm).callEvent(any(Event.class));
        assertFalse(LobbyService.sendToLobby(player, PlayerSendToLobbyEvent.Reason.LOBBY_COMMAND));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void listenerCanRedirectTheTarget() {
        Location other = mock(Location.class);
        doAnswer(inv -> {
            ((PlayerSendToLobbyEvent) inv.getArgument(0)).setTarget(other);
            return null;
        }).when(pm).callEvent(any(Event.class));
        assertTrue(LobbyService.sendToLobby(player, PlayerSendToLobbyEvent.Reason.MAP_RESET));
        verify(player).teleport(other);
    }

    @Test
    void nullTargetDoesNotTeleport() {
        doAnswer(inv -> {
            ((PlayerSendToLobbyEvent) inv.getArgument(0)).setTarget(null);
            return null;
        }).when(pm).callEvent(any(Event.class));
        assertFalse(LobbyService.sendToLobby(player, PlayerSendToLobbyEvent.Reason.RELOAD));
        verify(player, never()).teleport(any(Location.class));
    }
}
