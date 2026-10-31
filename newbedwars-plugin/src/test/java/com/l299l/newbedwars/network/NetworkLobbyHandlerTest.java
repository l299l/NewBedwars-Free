package com.l299l.newbedwars.network;

import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.common.protocol.MessageType;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.SendToLobby;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NetworkLobbyHandlerTest {
    Plugin plugin;
    Messenger messenger;
    Player player;
    BukkitScheduler scheduler;
    MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        messenger = mock(Messenger.class);
        player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        scheduler = mock(BukkitScheduler.class);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        ArenaRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        ArenaRegistry.clear();
    }

    private PlayerSendToLobbyEvent event(PlayerSendToLobbyEvent.Reason reason) {
        return new PlayerSendToLobbyEvent(player, reason, null);
    }

    private Runnable scheduled() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), task.capture(), eq(NetworkLobbyHandler.DELAY_TICKS));
        return task.getValue();
    }

    @Test
    void leftArenaGoesToConfiguredLobbyAfterDelay() {
        NetworkLobbyHandler h = new NetworkLobbyHandler(plugin, messenger, "game1", "lobby", p -> false);
        h.onSendToLobby(event(PlayerSendToLobbyEvent.Reason.LEFT_ARENA));
        verifyNoInteractions(messenger);
        scheduled().run();
        verify(messenger).connect(player, "lobby");
    }

    @Test
    void withoutLobbyServerTheProxyPicks() {
        NetworkLobbyHandler h = new NetworkLobbyHandler(plugin, messenger, "game1", "", p -> false);
        h.onSendToLobby(event(PlayerSendToLobbyEvent.Reason.LEFT_ARENA));
        scheduled().run();
        ArgumentCaptor<NetMessage> msg = ArgumentCaptor.forClass(NetMessage.class);
        verify(messenger).send(eq(player), msg.capture());
        assertEquals(MessageType.SEND_TO_LOBBY, msg.getValue().type());
        assertEquals(List.of(player.getUniqueId()), msg.getValue().payload(SendToLobby.class).players);
    }

    @Test
    void playerBackInAnArenaStays() {
        NetworkLobbyHandler h = new NetworkLobbyHandler(plugin, messenger, "game1", "lobby", p -> false);
        h.onSendToLobby(event(PlayerSendToLobbyEvent.Reason.LEFT_ARENA));
        IArena next = NetTestArenas.arena("next", GameStatus.waiting, 2, 1, 1, true);
        org.bukkit.World world = mock(org.bukkit.World.class);
        ArenaRegistry.register("next", next);
        ArenaRegistry.bindWorld(world, next);
        when(player.getWorld()).thenReturn(world);
        scheduled().run();
        verifyNoInteractions(messenger);
    }

    @Test
    void rejoinSlotAloneDoesNotKeepThePlayer() {
        NetworkLobbyHandler h = new NetworkLobbyHandler(plugin, messenger, "game1", "lobby", p -> false);
        IArena running = NetTestArenas.arena("running", GameStatus.playing, 2, 1, 1, true);
        when(running.isPlayerInArena(player)).thenReturn(true);
        ArenaRegistry.register("running", running);
        when(player.getWorld()).thenReturn(mock(org.bukkit.World.class));
        assertTrue(h.shouldLeave(player));
    }

    @Test
    void spectatorStays() {
        NetworkLobbyHandler h = new NetworkLobbyHandler(plugin, messenger, "game1", "lobby", p -> false);
        IArena a = NetTestArenas.arena("a", GameStatus.ending, 2, 1, 0, true);
        a.getSpectators().add(player);
        ArenaRegistry.register("a", a);
        assertFalse(h.shouldLeave(player));
    }

    @Test
    void waitingForTicketOrOfflineStays() {
        NetworkLobbyHandler h = new NetworkLobbyHandler(plugin, messenger, "game1", "lobby", p -> true);
        assertFalse(h.shouldLeave(player));
        NetworkLobbyHandler h2 = new NetworkLobbyHandler(plugin, messenger, "game1", "lobby", p -> false);
        when(player.isOnline()).thenReturn(false);
        assertFalse(h2.shouldLeave(player));
    }

    @Test
    void setupEndAndReloadStayShutdownIsImmediate() {
        NetworkLobbyHandler h = new NetworkLobbyHandler(plugin, messenger, "game1", "lobby", p -> false);
        h.onSendToLobby(event(PlayerSendToLobbyEvent.Reason.SETUP_ENDED));
        h.onSendToLobby(event(PlayerSendToLobbyEvent.Reason.RELOAD));
        verifyNoInteractions(scheduler, messenger);
        h.onSendToLobby(event(PlayerSendToLobbyEvent.Reason.SERVER_SHUTDOWN));
        verify(messenger).connect(player, "lobby");
        verifyNoInteractions(scheduler);
    }

    @Test
    void withoutLocalLobbyThePlayerWaitsInvisiblyAtTheWorldSpawn() {
        org.bukkit.World world = mock(org.bukkit.World.class);
        org.bukkit.Location spawn = mock(org.bukkit.Location.class);
        when(world.getSpawnLocation()).thenReturn(spawn);
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
        NetworkLobbyHandler h = new NetworkLobbyHandler(plugin, messenger, "game1", "lobby", p -> false, false);
        PlayerSendToLobbyEvent e = event(PlayerSendToLobbyEvent.Reason.LEFT_ARENA);
        h.onSendToLobby(e);
        assertSame(spawn, e.getTarget());
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), task.capture(), eq(NetworkLobbyHandler.SHORT_DELAY_TICKS));
        task.getValue().run();
        verify(player).setGameMode(org.bukkit.GameMode.SPECTATOR);
        verify(messenger).connect(player, "lobby");
    }
}
