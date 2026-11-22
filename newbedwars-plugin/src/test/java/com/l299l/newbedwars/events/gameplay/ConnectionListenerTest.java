package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.player.LobbyService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mockito.MockedStatic;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionListenerTest {
    private NewBedwars plugin;
    private ConnectionListener listener;
    private World arenaWorld;
    private World otherWorld;
    private IArena arena;
    private Player player;

    @BeforeEach
    void setUp() {
        plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        listener = new ConnectionListener(plugin);
        arenaWorld = mock(World.class);
        otherWorld = mock(World.class);
        arena = mock(IArena.class);
        when(arena.getArenaWorld()).thenReturn(arenaWorld);
        when(arena.status()).thenReturn(GameStatus.playing);
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        when(arena.getArenaName()).thenReturn("Castle");
        ArenaRegistry.register("Castle", arena);
        ArenaRegistry.bindWorld(arenaWorld, arena);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(otherWorld);
        when(arena.isPlayerInArena(player)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
        NewBedwars.plugin = null;
    }

    private PlayerQuitEvent quit() {
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    private PlayerChangedWorldEvent changed() {
        return new PlayerChangedWorldEvent(player, arenaWorld);
    }

    @Test
    void teleportingOutOfTheArenaWorldLeavesTheGame() {
        listener.onWorldChange(changed());
        verify(arena).leave(player, LeaveReason.OTHER);
    }

    @Test
    void spectatorsLeaveToo() {
        when(arena.isPlayerInArena(player)).thenReturn(false);
        when(arena.getSpectators()).thenReturn(new ArrayList<>(List.of(player)));
        listener.onWorldChange(changed());
        verify(arena).leave(player, LeaveReason.OTHER);
    }

    @Test
    void playersWaitingToRejoinAreIgnored() {
        Map<UUID, Long> slots = Map.of(player.getUniqueId(), 1L);
        when(arena.getRejoinSlots()).thenReturn(slots);
        listener.onWorldChange(changed());
        verify(arena, never()).leave(any(), any());
    }

    @Test
    void worldChangesInsideTheArenaWorldOrOutsideAGameAreIgnored() {
        when(player.getWorld()).thenReturn(arenaWorld);
        listener.onWorldChange(changed());
        when(player.getWorld()).thenReturn(otherWorld);
        when(arena.status()).thenReturn(GameStatus.restarting);
        listener.onWorldChange(changed());
        verify(arena, never()).leave(any(), any());
    }

    @Test
    void otherPlayersLeavingTheirWorldAreIgnored() {
        when(arena.isPlayerInArena(player)).thenReturn(false);
        listener.onWorldChange(changed());
        verify(arena, never()).leave(any(), any());
    }

    @Test
    void quitFindsTheArenaWhateverTheCurrentWorld() {
        listener.onLeave(quit());
        verify(arena).leave(player, LeaveReason.DISCONNECT);
    }

    @Test
    void quitSkipsPlayersAlreadyWaitingToRejoin() {
        Map<UUID, Long> slots = Map.of(player.getUniqueId(), 1L);
        when(arena.getRejoinSlots()).thenReturn(slots);
        listener.onLeave(quit());
        verify(arena, never()).leave(any(), any());
    }

    @Test
    void teleportingOutDuringTheEndingLeavesTheArena() {
        when(arena.isPlayerInArena(player)).thenReturn(false);
        when(arena.getSpectators()).thenReturn(new ArrayList<>(List.of(player)));
        when(arena.status()).thenReturn(GameStatus.ending);
        listener.onWorldChange(changed());
        verify(arena).leave(player, LeaveReason.OTHER);
    }

    private void enterArenaWorld(boolean sentToLobby) {
        when(player.getWorld()).thenReturn(arenaWorld);
        when(player.isOnline()).thenReturn(true);
        when(arena.isEnabled()).thenReturn(true);
        when(arena.getPlayersOnSetup()).thenReturn(new ArrayList<>());
        Messages messages = mock(Messages.class);
        when(plugin.getMessages()).thenReturn(messages);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(inv -> {
            inv.getArgument(1, Runnable.class).run();
            return null;
        });
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<LobbyService> lobby = mockStatic(LobbyService.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            lobby.when(() -> LobbyService.sendToLobby(any(), any())).thenReturn(true);
            listener.onWorldChange(new PlayerChangedWorldEvent(player, otherWorld));
            lobby.verify(() -> LobbyService.sendToLobby(eq(player), eq(PlayerSendToLobbyEvent.Reason.LEFT_ARENA)),
                    sentToLobby ? times(1) : never());
            verify(messages, sentToLobby ? times(1) : never()).send(player, "NotInThisArena");
        }
    }

    @Test
    void outsidersEnteringAnEnabledArenaWorldAreSentToTheLobby() {
        when(arena.isPlayerInArena(player)).thenReturn(false);
        enterArenaWorld(true);
    }

    @Test
    void participantsEnteringTheirArenaWorldStay() {
        enterArenaWorld(false);
    }

    @Test
    void adminsEnteringAnArenaWorldStay() {
        when(arena.isPlayerInArena(player)).thenReturn(false);
        when(player.hasPermission(ConnectionListener.ADMIN_PERMISSION)).thenReturn(true);
        enterArenaWorld(false);
    }

    @Test
    void outsiderCheckSkipsDisabledArenasAndSetupPlayers() {
        when(arena.isPlayerInArena(player)).thenReturn(false);
        java.util.UUID id = player.getUniqueId();
        when(arena.getPlayersOnSetup()).thenReturn(new ArrayList<>(List.of(id)));
        when(arena.isEnabled()).thenReturn(true);
        assertFalse(ConnectionListener.isOutsider(arena, player));
        when(arena.getPlayersOnSetup()).thenReturn(new ArrayList<>());
        assertTrue(ConnectionListener.isOutsider(arena, player));
        when(arena.isEnabled()).thenReturn(false);
        assertFalse(ConnectionListener.isOutsider(arena, player));
    }
}
