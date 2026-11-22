package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.NewBedwars;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CombatListenerTest {

    private static EntityDamageEvent damage(DamageCause cause) {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getCause()).thenReturn(cause);
        return event;
    }

    @Test
    void quickVoidIsVoidWithoutAnAttacker() {
        assertEquals(DamageCause.VOID, CombatListener.deathCause(damage(DamageCause.CUSTOM), true));
    }

    @Test
    void quickVoidIsVoidWhenAPlayerKnockedTheVictimOff() {
        assertEquals(DamageCause.VOID, CombatListener.deathCause(damage(DamageCause.ENTITY_ATTACK), true));
    }

    @Test
    void otherDeathsKeepTheirCause() {
        assertEquals(DamageCause.FALL, CombatListener.deathCause(damage(DamageCause.FALL), false));
        assertEquals(DamageCause.VOID, CombatListener.deathCause(damage(DamageCause.VOID), false));
    }

    @Test
    void missingCauseIsCustom() {
        assertEquals(DamageCause.CUSTOM, CombatListener.deathCause(null, false));
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
    }

    private Player player(World world) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        return player;
    }

    private EntityDamageByEntityEvent hit(Player victim, Player attacker) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(attacker);
        return event;
    }

    private IArena arena(World world, Player victim, Player attacker, Team victimTeam, Team attackerTeam) {
        IArena arena = mock(IArena.class);
        when(arena.status()).thenReturn(GameStatus.playing);
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        when(arena.isPlayerInArena(victim)).thenReturn(true);
        when(arena.isPlayerInArena(attacker)).thenReturn(true);
        when(arena.getTeam(victim)).thenReturn(victimTeam);
        when(arena.getTeam(attacker)).thenReturn(attackerTeam);
        ArenaRegistry.bindWorld(world, arena);
        return arena;
    }

    @Test
    void damageCreditIsRecordedOnlyAfterTheHitSurvivesCancellation() throws Exception {
        World world = mock(World.class);
        Player victim = player(world);
        Player attacker = player(world);
        arena(world, victim, attacker, mock(Team.class), mock(Team.class));
        CombatTracker tracker = mock(CombatTracker.class);
        CombatListener listener = new CombatListener(tracker);
        EntityDamageByEntityEvent event = hit(victim, attacker);

        listener.onTeamDamage(event);
        verify(tracker, never()).record(any(), any(), anyBoolean());

        EventHandler credit = CombatListener.class.getMethod("onDamageCredit", EntityDamageByEntityEvent.class)
                .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.MONITOR, credit.priority());
        assertTrue(credit.ignoreCancelled());

        listener.onDamageCredit(event);
        verify(tracker).record(victim.getUniqueId(), attacker.getUniqueId(), false);
    }

    @Test
    void teammateHitsAreNeverCredited() {
        World world = mock(World.class);
        Player victim = player(world);
        Player attacker = player(world);
        Team team = mock(Team.class);
        arena(world, victim, attacker, team, team);
        CombatTracker tracker = mock(CombatTracker.class);

        new CombatListener(tracker).onDamageCredit(hit(victim, attacker));

        verify(tracker, never()).record(any(), any(), anyBoolean());
    }

    private void makeLethal(Player victim, EntityDamageByEntityEvent event) {
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack empty = mock(ItemStack.class);
        when(empty.getType()).thenReturn(Material.AIR);
        when(inventory.getItemInMainHand()).thenReturn(empty);
        when(inventory.getItemInOffHand()).thenReturn(empty);
        when(victim.getInventory()).thenReturn(inventory);
        when(victim.getHealth()).thenReturn(5.0);
        when(event.getFinalDamage()).thenReturn(30.0);
        when(event.getCause()).thenReturn(DamageCause.ENTITY_EXPLOSION);
    }

    @Test
    void lethalTntHitCreditsTheOwnerBeforeTheDeathIsHandled() {
        World world = mock(World.class);
        Player victim = player(world);
        Player owner = player(world);
        arena(world, victim, owner, mock(Team.class), mock(Team.class));
        TNTPrimed tnt = mock(TNTPrimed.class);
        when(tnt.getSource()).thenReturn(owner);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(tnt);
        makeLethal(victim, event);
        CombatTracker tracker = mock(CombatTracker.class);
        CombatListener listener = spy(new CombatListener(tracker));
        doNothing().when(listener).handleDeath(any(), any(), any(), any());

        listener.onLethalDamage(event);

        InOrder order = inOrder(tracker, listener);
        order.verify(tracker).record(victim.getUniqueId(), owner.getUniqueId(), false);
        order.verify(listener).handleDeath(eq(victim), any(), eq(event), eq(null));
        verify(event).setCancelled(true);
    }

    @Test
    void lethalGolemHitCreditsTheOwnerAsAMobHit() {
        World world = mock(World.class);
        Player victim = player(world);
        Player owner = player(world);
        arena(world, victim, owner, mock(Team.class), mock(Team.class));
        NewBedwars previous = NewBedwars.plugin;
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getName()).thenReturn("NewBedwars");
        NewBedwars.plugin = plugin;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(owner.getUniqueId())).thenReturn(owner);
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenAnswer(i -> {
                NamespacedKey key = i.getArgument(0);
                return key.getKey().equals("team_golem_owner") ? owner.getUniqueId().toString() : "Arena;Red";
            });
            IronGolem golem = mock(IronGolem.class);
            when(golem.getPersistentDataContainer()).thenReturn(pdc);
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            when(event.getEntity()).thenReturn(victim);
            when(event.getDamager()).thenReturn(golem);
            makeLethal(victim, event);
            CombatTracker tracker = mock(CombatTracker.class);
            CombatListener listener = spy(new CombatListener(tracker));
            doNothing().when(listener).handleDeath(any(), any(), any(), any());

            listener.onLethalDamage(event);

            InOrder order = inOrder(tracker, listener);
            order.verify(tracker).record(victim.getUniqueId(), owner.getUniqueId(), true);
            order.verify(listener).handleDeath(eq(victim), any(), eq(event), eq(null));
        } finally {
            NewBedwars.plugin = previous;
        }
    }

    @Test
    void lethalHitFromATeammateIsNotCredited() {
        World world = mock(World.class);
        Player victim = player(world);
        Player attacker = player(world);
        Team team = mock(Team.class);
        arena(world, victim, attacker, team, team);
        EntityDamageByEntityEvent event = hit(victim, attacker);
        makeLethal(victim, event);
        CombatTracker tracker = mock(CombatTracker.class);
        CombatListener listener = spy(new CombatListener(tracker));
        doNothing().when(listener).handleDeath(any(), any(), any(), any());

        listener.onLethalDamage(event);

        verify(tracker, never()).record(any(), any(), anyBoolean());
        verify(listener).handleDeath(eq(victim), any(), eq(event), eq(attacker));
    }

    private EntityDamageByEntityEvent fireballHit(Player victim, Player shooter) {
        Fireball fireball = mock(Fireball.class);
        when(fireball.getShooter()).thenReturn(shooter);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(fireball);
        return event;
    }

    @Test
    void ownFireballIsNotCancelledSoTheJumpKeepsItsKnockback() {
        World world = mock(World.class);
        Player victim = player(world);
        Team team = mock(Team.class);
        arena(world, victim, victim, team, team);
        EntityDamageByEntityEvent event = fireballHit(victim, victim);

        new CombatListener(mock(CombatTracker.class)).onTeamDamage(event);

        verify(event, never()).setCancelled(true);
        verify(event, never()).setDamage(anyDouble());
    }

    @Test
    void teammateFireballDealsNoDamageButKeepsKnockback() {
        World world = mock(World.class);
        Player victim = player(world);
        Player attacker = player(world);
        Team team = mock(Team.class);
        arena(world, victim, attacker, team, team);
        EntityDamageByEntityEvent event = fireballHit(victim, attacker);
        CombatTracker tracker = mock(CombatTracker.class);
        CombatListener listener = new CombatListener(tracker);

        listener.onTeamDamage(event);
        listener.onDamageCredit(event);

        verify(event, never()).setCancelled(true);
        verify(event).setDamage(0);
        verify(tracker, never()).record(any(), any(), anyBoolean());
    }

    @Test
    void teammateMeleeHitsAreStillCancelled() {
        World world = mock(World.class);
        Player victim = player(world);
        Player attacker = player(world);
        Team team = mock(Team.class);
        arena(world, victim, attacker, team, team);
        EntityDamageByEntityEvent event = hit(victim, attacker);

        new CombatListener(mock(CombatTracker.class)).onTeamDamage(event);

        verify(event).setCancelled(true);
    }

    @Test
    void projectilesPassThroughSpectators() {
        World world = mock(World.class);
        Player spectator = player(world);
        Player fighter = player(world);
        IArena arena = mock(IArena.class);
        when(arena.getSpectators()).thenReturn(new ArrayList<>(java.util.List.of(spectator)));
        ArenaRegistry.bindWorld(world, arena);
        CombatListener listener = new CombatListener(mock(CombatTracker.class));
        Arrow arrow = mock(Arrow.class);

        ProjectileHitEvent onSpectator = new ProjectileHitEvent(arrow, spectator);
        listener.onProjectileHitSpectator(onSpectator);
        ProjectileHitEvent onFighter = new ProjectileHitEvent(arrow, fighter);
        listener.onProjectileHitSpectator(onFighter);

        assertTrue(onSpectator.isCancelled());
        assertFalse(onFighter.isCancelled());
    }
}
