package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class SpectatorMobAndShopTest {
    private final World world = mock(World.class);
    private final IArena arena = mock(IArena.class);
    private final Player spectator = mock(Player.class);

    SpectatorMobAndShopTest() {
        when(spectator.getWorld()).thenReturn(world);
        when(arena.getSpectators()).thenReturn(new java.util.ArrayList<>(List.of(spectator)));
        when(arena.status()).thenReturn(GameStatus.playing);
        when(arena.isPlayerInArena(spectator)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
    }

    private EntityDamageByEntityEvent hit(Entity victim) {
        EntityDamageByEntityEvent e = mock(EntityDamageByEntityEvent.class);
        when(e.getEntity()).thenReturn(victim);
        when(e.getDamager()).thenReturn(spectator);
        return e;
    }

    @Test
    void mobListenerCancelsSpectatorHitsOnGolemAndSilverfish() {
        try (var reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            for (Entity mob : new Entity[]{mock(IronGolem.class), mock(Silverfish.class)}) {
                EntityDamageByEntityEvent e = hit(mob);
                new MobListener().onMobDamage(e);
                verify(e).setCancelled(true);
            }
        }
    }

    @Test
    void combatListenerCancelsSpectatorHitsOnGolem() {
        try (var reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            EntityDamageByEntityEvent e = hit(mock(IronGolem.class));
            new CombatListener(mock(CombatTracker.class)).onTeamDamage(e);
            verify(e).setCancelled(true);
        }
    }

    @Test
    void spectatorCannotOpenShop() {
        try (var reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            PlayerInteractEntityEvent e = mock(PlayerInteractEntityEvent.class);
            when(e.getHand()).thenReturn(EquipmentSlot.HAND);
            when(e.getPlayer()).thenReturn(spectator);
            new InventoryListener().playerNpcInteract(e);
            verify(arena, never()).getTeamShops();
            verify(e, never()).getRightClicked();
        }
    }
}
