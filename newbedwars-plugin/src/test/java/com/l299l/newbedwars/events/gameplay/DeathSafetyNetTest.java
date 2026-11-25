package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class DeathSafetyNetTest {

    private World world;
    private IArena arena;
    private Player player;
    private ArrayList<Player> spectators;
    private Location spawn;
    private CombatListener listener;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        arena = mock(IArena.class);
        spectators = new ArrayList<>();
        spawn = mock(Location.class);
        when(arena.getSpectators()).thenReturn(spectators);
        when(arena.getWaitingSpawn()).thenReturn(spawn);
        when(arena.getQuickVoidY()).thenReturn(40);
        when(arena.getPlayers()).thenReturn(Set.of(id));
        ArenaRegistry.register("Test", arena);
        ArenaRegistry.bindWorld(world, arena);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getWorld()).thenReturn(world);
        when(player.isOnline()).thenReturn(true);
        when(player.getHealth()).thenReturn(10.0);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack air = mock(ItemStack.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(inventory.getItemInMainHand()).thenReturn(air);
        when(inventory.getItemInOffHand()).thenReturn(air);
        when(player.getInventory()).thenReturn(inventory);
        when(arena.isPlayerInArena(player)).thenReturn(true);
        when(arena.getTeam(player)).thenReturn(mock(Team.class));

        listener = spy(new CombatListener(new CombatTracker()));
        doNothing().when(listener).handleDeath(any(), any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
    }

    private EntityDamageEvent damage(DamageCause cause, double finalDamage) {
        EntityDamageEvent e = mock(EntityDamageEvent.class);
        when(e.getEntity()).thenReturn(player);
        when(e.getCause()).thenReturn(cause);
        when(e.getFinalDamage()).thenReturn(finalDamage);
        return e;
    }

    private void at(double y) {
        Location loc = mock(Location.class);
        when(loc.getY()).thenReturn(y);
        when(player.getLocation()).thenReturn(loc);
    }

    @Test
    void lethalDamageIsCancelledAndHandledAsADeath() {
        when(arena.status()).thenReturn(GameStatus.playing);
        EntityDamageEvent e = damage(DamageCause.FALL, 12.0);
        listener.onLethalDamage(e);
        verify(e).setCancelled(true);
        verify(listener).handleDeath(eq(player), eq(arena), eq(e), isNull());
    }

    @Test
    void nonLethalDamageIsLeftAlone() {
        when(arena.status()).thenReturn(GameStatus.playing);
        EntityDamageEvent e = damage(DamageCause.FALL, 4.0);
        listener.onLethalDamage(e);
        verify(e, never()).setCancelled(anyBoolean());
        verify(listener, never()).handleDeath(any(), any(), any(), any());
    }

    @Test
    void anyVoidDamageIsAVoidDeath() {
        when(arena.status()).thenReturn(GameStatus.playing);
        EntityDamageEvent e = damage(DamageCause.VOID, 4.0);
        listener.onLethalDamage(e);
        verify(e).setCancelled(true);
        verify(listener).handleDeath(eq(player), eq(arena), eq(e), isNull());
    }

    @Test
    void totemHolderIsLeftToVanilla() {
        when(arena.status()).thenReturn(GameStatus.playing);
        ItemStack totem = mock(ItemStack.class);
        when(totem.getType()).thenReturn(Material.TOTEM_OF_UNDYING);
        when(player.getInventory().getItemInOffHand()).thenReturn(totem);
        EntityDamageEvent e = damage(DamageCause.ENTITY_ATTACK, 50.0);
        listener.onLethalDamage(e);
        verify(listener, never()).handleDeath(any(), any(), any(), any());
    }

    @Test
    void spectatorsAndWaitingPlayersAreNotKilled() {
        when(arena.status()).thenReturn(GameStatus.waiting);
        listener.onLethalDamage(damage(DamageCause.VOID, 100.0));
        when(arena.status()).thenReturn(GameStatus.playing);
        spectators.add(player);
        listener.onLethalDamage(damage(DamageCause.VOID, 100.0));
        verify(listener, never()).handleDeath(any(), any(), any(), any());
    }

    @Test
    void deathThatSlipsThroughIsCancelledAndHandled() {
        when(arena.status()).thenReturn(GameStatus.playing);
        PlayerDeathEvent e = mock(PlayerDeathEvent.class);
        when(e.getEntity()).thenReturn(player);
        listener.playerDeathEvent(e);
        verify(e).setCancelled(true);
        verify(e).setReviveHealth(anyDouble());
        verify(listener).handleDeath(eq(player), eq(arena), any(), isNull());
    }

    @Test
    void deathWhileWaitingIsCancelledAndRescued() {
        when(arena.status()).thenReturn(GameStatus.starting);
        PlayerDeathEvent e = mock(PlayerDeathEvent.class);
        when(e.getEntity()).thenReturn(player);
        listener.playerDeathEvent(e);
        verify(e).setCancelled(true);
        verify(player).teleport(spawn);
        verify(listener, never()).handleDeath(any(), any(), any(), any());
    }

    @Test
    void deathOutsideArenasIsIgnored() {
        when(player.getWorld()).thenReturn(mock(World.class));
        PlayerDeathEvent e = mock(PlayerDeathEvent.class);
        when(e.getEntity()).thenReturn(player);
        listener.playerDeathEvent(e);
        verify(e, never()).setCancelled(anyBoolean());
    }

    @Test
    void lobbyDeathMessageOnlyReachesPlayersOutsideArenas() {
        World lobbyWorld = mock(World.class);
        Player lobbyPlayer = mock(Player.class);
        when(lobbyPlayer.getWorld()).thenReturn(lobbyWorld);
        Player inArena = mock(Player.class);
        World arenaWorld = player.getWorld();
        when(inArena.getWorld()).thenReturn(arenaWorld);
        PlayerDeathEvent e = mock(PlayerDeathEvent.class);
        when(e.getDeathMessage()).thenReturn("Steve fell");
        org.bukkit.command.ConsoleCommandSender console = mock(org.bukkit.command.ConsoleCommandSender.class);
        try (org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenReturn(java.util.List.of(lobbyPlayer, inArena));
            bukkit.when(org.bukkit.Bukkit::getConsoleSender).thenReturn(console);
            CombatListener.keepDeathMessageOutOfArenas(e);
        }
        verify(e).setDeathMessage(null);
        verify(lobbyPlayer).sendMessage("Steve fell");
        verify(inArena, never()).sendMessage(anyString());
        verify(console).sendMessage("Steve fell");
    }

    @Test
    void lobbyDeathMessageIsUntouchedWhenNobodyIsInAnArena() {
        Player lobbyPlayer = mock(Player.class);
        when(lobbyPlayer.getWorld()).thenReturn(mock(World.class));
        PlayerDeathEvent e = mock(PlayerDeathEvent.class);
        when(e.getDeathMessage()).thenReturn("Steve fell");
        try (org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenReturn(java.util.List.of(lobbyPlayer));
            CombatListener.keepDeathMessageOutOfArenas(e);
        }
        verify(e, never()).setDeathMessage(any());
        verify(lobbyPlayer, never()).sendMessage(anyString());
    }

    @Test
    void lobbyDeathMessageRespectsTheShowDeathMessagesGamerule() {
        World lobbyWorld = mock(World.class);
        when(lobbyWorld.getGameRuleValue(org.bukkit.GameRule.SHOW_DEATH_MESSAGES)).thenReturn(false);
        Player dead = mock(Player.class);
        when(dead.getWorld()).thenReturn(lobbyWorld);
        Player inArena = mock(Player.class);
        World arenaWorld = player.getWorld();
        when(inArena.getWorld()).thenReturn(arenaWorld);
        PlayerDeathEvent e = mock(PlayerDeathEvent.class);
        when(e.getEntity()).thenReturn(dead);
        when(e.getDeathMessage()).thenReturn("Steve fell");
        try (org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenReturn(java.util.List.of(dead, inArena));
            CombatListener.keepDeathMessageOutOfArenas(e);
        }
        verify(e, never()).setDeathMessage(any());
        verify(dead, never()).sendMessage(anyString());
    }

    @Test
    void safetyTaskKillsAPlayerBelowTheVoidLine() {
        when(arena.status()).thenReturn(GameStatus.playing);
        doNothing().when(listener).voidDeath(any(), any());
        at(39.0);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
            new ArenaSafetyTask(listener).run();
        }
        verify(listener).voidDeath(player, arena);
    }

    @Test
    void safetyTaskLeavesPlayersAboveTheVoidLine() {
        when(arena.status()).thenReturn(GameStatus.playing);
        doNothing().when(listener).voidDeath(any(), any());
        at(64.0);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
            new ArenaSafetyTask(listener).run();
        }
        verify(listener, never()).voidDeath(any(), any());
    }

    @Test
    void safetyTaskRestartsAStuckRespawn() {
        when(arena.status()).thenReturn(GameStatus.playing);
        spectators.add(player);
        at(100.0);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
            new ArenaSafetyTask(listener).run();
        }
        verify(arena).ensureRespawning(player);
        verify(listener, never()).voidDeath(any(), any());
    }

    @Test
    void safetyTaskRescuesWaitingPlayersFromTheVoid() {
        when(arena.status()).thenReturn(GameStatus.waiting);
        at(10.0);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
            new ArenaSafetyTask(listener).run();
        }
        verify(player).teleport(spawn);
    }

    @Test
    void voidDeathWithoutTeamRescuesInsteadOfThrowing() {
        CombatListener real = new CombatListener(new CombatTracker());
        when(arena.getTeam(player)).thenReturn(null);
        real.handleDeath(player, arena, null, null);
        verify(player).teleport(spawn);
        verify(arena, never()).killPlayer(any());
    }
}
