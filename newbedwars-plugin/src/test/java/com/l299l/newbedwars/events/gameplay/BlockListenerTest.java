package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockListenerTest {
    private BlockListener listener;
    private IArena arena;
    private Player player;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        listener = new BlockListener(plugin);
        World world = mock(World.class);
        arena = mock(IArena.class);
        when(arena.isEnabled()).thenReturn(true);
        when(arena.status()).thenReturn(GameStatus.playing);
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        when(arena.getPlayersOnSetup()).thenReturn(new ArrayList<>());
        ArenaRegistry.bindWorld(world, arena);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
        NewBedwars.plugin = null;
    }

    private BlockPlaceEvent place() {
        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    private BlockBreakEvent breakBlock() {
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    @Test
    void outsidersCannotBuildOrBreakInAnEnabledArena() {
        BlockPlaceEvent placeEvent = place();
        BlockBreakEvent breakEvent = breakBlock();
        listener.playerPlaceBlockEvent(placeEvent);
        listener.playerBreakBlockEvent(breakEvent);
        verify(placeEvent).setCancelled(true);
        verify(breakEvent).setCancelled(true);
    }

    @Test
    void adminsAndSetupPlayersAreNotBlocked() {
        when(player.hasPermission(ConnectionListener.ADMIN_PERMISSION)).thenReturn(true);
        BlockPlaceEvent asAdmin = place();
        listener.playerPlaceBlockEvent(asAdmin);
        verify(asAdmin, never()).setCancelled(true);

        when(player.hasPermission(ConnectionListener.ADMIN_PERMISSION)).thenReturn(false);
        java.util.UUID id = player.getUniqueId();
        when(arena.getPlayersOnSetup()).thenReturn(new ArrayList<>(List.of(id)));
        BlockPlaceEvent inSetup = place();
        listener.playerPlaceBlockEvent(inSetup);
        verify(inSetup, never()).setCancelled(true);
    }

    @Test
    void disabledArenasAreNotAffected() {
        when(arena.isEnabled()).thenReturn(false);
        BlockBreakEvent event = breakBlock();
        listener.playerBreakBlockEvent(event);
        verify(event, never()).setCancelled(true);
    }
}
