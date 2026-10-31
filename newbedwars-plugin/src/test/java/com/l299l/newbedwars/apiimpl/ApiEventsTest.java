package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.events.*;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiEventsTest {
    private MockedStatic<Bukkit> bukkit;
    private PluginManager pm;
    private IArena arena;
    private Team team;
    private Player player;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        pm = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
        arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("A1");
        team = new Team(ChatColor.RED, "Red", arena);
        player = mock(Player.class);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private <E extends Event> E captured(Class<E> type) {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pm).callEvent(captor.capture());
        assertInstanceOf(type, captor.getValue());
        return type.cast(captor.getValue());
    }

    private void cancelEverything() {
        doAnswer(inv -> {
            Event e = inv.getArgument(0);
            if (e instanceof Cancellable c) c.setCancelled(true);
            return null;
        }).when(pm).callEvent(any(Event.class));
    }

    @Test
    void gameStartCarriesArena() {
        ApiEvents.gameStart(arena);
        assertEquals("A1", captured(GameStartEvent.class).getArena().getName());
    }

    @Test
    void gameEndCarriesWinnerOrNullOnDraw() {
        ApiEvents.gameEnd(arena, team);
        assertEquals("Red", captured(GameEndEvent.class).getWinner().getName());
    }

    @Test
    void gameEndDrawHasNoWinner() {
        ApiEvents.gameEnd(arena, null);
        assertNull(captured(GameEndEvent.class).getWinner());
    }

    @Test
    void arenaResetCarriesStage() {
        ApiEvents.arenaReset(arena, ArenaResetEvent.Stage.POST);
        assertEquals(ArenaResetEvent.Stage.POST, captured(ArenaResetEvent.class).getStage());
    }

    @Test
    void joinIsAllowedByDefaultAndCarriesRejoinFlag() {
        assertTrue(ApiEvents.playerJoin(arena, player, true));
        PlayerJoinArenaEvent e = captured(PlayerJoinArenaEvent.class);
        assertTrue(e.isRejoin());
        assertSame(player, e.getPlayer());
    }

    @Test
    void cancelledJoinIsReported() {
        cancelEverything();
        assertFalse(ApiEvents.playerJoin(arena, player, false));
    }

    @Test
    void leaveCarriesReason() {
        ApiEvents.playerLeave(arena, player, LeaveReason.DISCONNECT);
        assertEquals(LeaveReason.DISCONNECT, captured(PlayerLeaveArenaEvent.class).getReason());
    }

    @Test
    void bedBreakCancelIsReported() {
        assertTrue(ApiEvents.bedBreak(arena, team, player));
        cancelEverything();
        assertFalse(ApiEvents.bedBreak(arena, team, player));
    }

    @Test
    void killCarriesFinalFlagKillerAndCause() {
        Player killer = mock(Player.class);
        ApiEvents.playerKill(arena, player, killer, true, DamageCause.VOID);
        PlayerKillEvent e = captured(PlayerKillEvent.class);
        assertSame(player, e.getVictim());
        assertSame(killer, e.getKiller());
        assertTrue(e.isFinalKill());
        assertEquals(DamageCause.VOID, e.getCause());
    }

    @Test
    void killerMayBeNull() {
        ApiEvents.playerKill(arena, player, null, false, DamageCause.FALL);
        assertNull(captured(PlayerKillEvent.class).getKiller());
    }

    @Test
    void shopPurchaseCarriesDetailsAndCanBeCancelled() {
        assertTrue(ApiEvents.shopPurchase(arena, player, "Wool", ShopPurchaseEvent.Kind.ITEM, "IRON", 4));
        cancelEverything();
        assertFalse(ApiEvents.shopPurchase(arena, player, "Wool", ShopPurchaseEvent.Kind.ITEM, "IRON", 4));
    }

    @Test
    void teamUpgradeCarriesLevelAndCanBeCancelled() {
        cancelEverything();
        assertFalse(ApiEvents.teamUpgrade(arena, team, "SHARPNESS", 2, player));
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pm).callEvent(captor.capture());
        TeamUpgradeEvent e = (TeamUpgradeEvent) captor.getValue();
        assertEquals("SHARPNESS", e.getUpgrade());
        assertEquals(2, e.getNewLevel());
        assertEquals("Red", e.getTeam().getName());
    }

    @Test
    void armorEquipReturnsArmorReplacedByListener() {
        ItemStack[] original = new ItemStack[4];
        ItemStack[] replacement = new ItemStack[4];
        doAnswer(inv -> {
            ((ArmorEquipEvent) inv.getArgument(0)).setArmor(replacement);
            return null;
        }).when(pm).callEvent(any(Event.class));
        assertSame(replacement, ApiEvents.armorEquip(arena, player, original));
    }

    @Test
    void armorEquipKeepsArmorWhenNobodyChangesIt() {
        ItemStack[] original = new ItemStack[4];
        assertSame(original, ApiEvents.armorEquip(arena, player, original));
    }

    @Test
    void armorEquipRejectsWrongSize() {
        ApiEvents.armorEquip(arena, player, new ItemStack[4]);
        ArmorEquipEvent e = captured(ArmorEquipEvent.class);
        assertThrows(IllegalArgumentException.class, () -> e.setArmor(new ItemStack[3]));
    }

    @Test
    void mapVoteCarriesVoterAndChoiceAndCanBeCancelled() {
        IArena voted = mock(IArena.class);
        when(voted.getArenaName()).thenReturn("B2");
        assertTrue(ApiEvents.mapVote(arena, player, voted));
        com.l299l.newbedwars.api.events.MapVoteEvent e = captured(com.l299l.newbedwars.api.events.MapVoteEvent.class);
        assertSame(player, e.getPlayer());
        assertEquals("A1", e.getArena().getName());
        assertEquals("B2", e.getVotedArena().getName());
    }

    @Test
    void cancelledMapVoteIsReported() {
        cancelEverything();
        assertFalse(ApiEvents.mapVote(arena, player, arena));
    }

    @Test
    void mapChosenCarriesGroupAndChosenArena() {
        IArena chosen = mock(IArena.class);
        when(chosen.getArenaName()).thenReturn("B2");
        com.l299l.newbedwars.arena.group.ArenaGroup group = new com.l299l.newbedwars.arena.group.ArenaGroup("solo", null);
        group.add("A1");
        group.add("B2");
        ApiEvents.mapChosen(arena, group, chosen);
        com.l299l.newbedwars.api.events.MapChosenEvent e = captured(com.l299l.newbedwars.api.events.MapChosenEvent.class);
        assertEquals("A1", e.getArena().getName());
        assertEquals("B2", e.getChosenArena().getName());
        assertEquals("solo", e.getGroup().getName());
        assertEquals(java.util.List.of("A1", "B2"), e.getGroup().getArenaNames());
    }
}
