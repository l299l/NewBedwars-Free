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
    void phaseChangeCarriesNamesAndForcedFlag() {
        ApiEvents.phaseChange(arena, "Diamond II", "Beds gone", true);
        GamePhaseChangeEvent e = captured(GamePhaseChangeEvent.class);
        assertEquals("Diamond II", e.getPrevious());
        assertEquals("Beds gone", e.getNext());
        assertTrue(e.isForced());
    }

    @Test
    void arenaChatCarriesMessageAndTeam() {
        ApiEvents.arenaChat(arena, player, "gg", true, team);
        ArenaChatEvent e = captured(ArenaChatEvent.class);
        assertSame(player, e.getPlayer());
        assertEquals("gg", e.getMessage());
        assertTrue(e.isTeamChat());
        assertEquals("Red", e.getTeam().getName());
    }

    @Test
    void bedsDestroyedListsTeams() {
        ApiEvents.bedsDestroyed(arena, java.util.List.of(team));
        BedsDestroyedEvent e = captured(BedsDestroyedEvent.class);
        assertEquals(1, e.getTeams().size());
        assertThrows(UnsupportedOperationException.class, () -> e.getTeams().clear());
    }

    @Test
    void teamEliminatedCarriesTeam() {
        ApiEvents.teamEliminated(arena, team);
        assertEquals("Red", captured(TeamEliminatedEvent.class).getTeam().getName());
    }

    @Test
    void generatorUpgradeMapsKind() {
        ApiEvents.generatorUpgrade(arena, com.l299l.newbedwars.arena.generators.GeneratorType.EMERALD, 3);
        GeneratorUpgradeEvent e = captured(GeneratorUpgradeEvent.class);
        assertEquals(com.l299l.newbedwars.api.view.GeneratorKind.EMERALD, e.getKind());
        assertEquals(3, e.getLevel());
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
        ApiEvents.playerKill(arena, player, killer, true, DamageCause.VOID, null);
        PlayerKillEvent e = captured(PlayerKillEvent.class);
        assertSame(player, e.getVictim());
        assertSame(killer, e.getKiller());
        assertTrue(e.isFinalKill());
        assertEquals(DamageCause.VOID, e.getCause());
        assertTrue(e.getDragonTeam().isEmpty());
    }

    @Test
    void killByDragonCarriesTheDragonTeam() {
        ApiEvents.playerKill(arena, player, null, false, DamageCause.ENTITY_EXPLOSION, team);
        assertEquals(team.getName(), captured(PlayerKillEvent.class).getDragonTeam().orElseThrow().getName());
    }

    @Test
    void killerMayBeNull() {
        ApiEvents.playerKill(arena, player, null, false, DamageCause.FALL, null);
        assertNull(captured(PlayerKillEvent.class).getKiller());
    }

    @Test
    void shopPurchaseCarriesDetailsAndCanBeCancelled() {
        assertEquals(4, ApiEvents.shopPurchase(arena, player, "Wool", ShopPurchaseEvent.Kind.ITEM, "IRON", 4, null));
        cancelEverything();
        assertEquals(-1, ApiEvents.shopPurchase(arena, player, "Wool", ShopPurchaseEvent.Kind.ITEM, "IRON", 4, null));
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

    @Test
    void stateChangeMapsBothStates() {
        ApiEvents.stateChange(arena, com.l299l.newbedwars.arena.GameStatus.waiting, com.l299l.newbedwars.arena.GameStatus.starting);
        com.l299l.newbedwars.api.events.ArenaStateChangeEvent e = captured(com.l299l.newbedwars.api.events.ArenaStateChangeEvent.class);
        assertEquals(com.l299l.newbedwars.api.view.ArenaState.WAITING, e.getPrevious());
        assertEquals(com.l299l.newbedwars.api.view.ArenaState.STARTING, e.getNext());
    }

    @Test
    void shopPurchaseReturnsThePriceAListenerSet() {
        doAnswer(inv -> {
            if (inv.getArgument(0) instanceof ShopPurchaseEvent e) e.setPrice(1);
            return null;
        }).when(pm).callEvent(any(Event.class));
        assertEquals(1, ApiEvents.shopPurchase(arena, player, "Wool", ShopPurchaseEvent.Kind.ITEM, "IRON", 4, team));
    }

    @Test
    void shopPurchaseCarriesTheTeam() {
        ApiEvents.shopPurchase(arena, player, "Wool", ShopPurchaseEvent.Kind.ITEM, "IRON", 4, team);
        assertEquals("Red", captured(ShopPurchaseEvent.class).getTeam().orElseThrow().getName());
    }

    @Test
    void negativePricesAreRefused() {
        ShopPurchaseEvent e = new ShopPurchaseEvent(null, player, "Wool", ShopPurchaseEvent.Kind.ITEM, "IRON", 4);
        assertThrows(IllegalArgumentException.class, () -> e.setPrice(-1));
        assertTrue(e.getTeam().isEmpty());
    }

    @Test
    void trapTriggerAndSpectateCanBeCancelled() {
        assertTrue(ApiEvents.trapTrigger(arena, team, player, "ALARMTRAP"));
        assertTrue(ApiEvents.spectate(arena, player));
        cancelEverything();
        assertFalse(ApiEvents.trapTrigger(arena, team, player, "ALARMTRAP"));
        assertFalse(ApiEvents.spectate(arena, player));
    }

    @Test
    void trapTriggerCarriesTheTrapAndTeam() {
        ApiEvents.trapTrigger(arena, team, player, "BLINDTRAP");
        com.l299l.newbedwars.api.events.TrapTriggerEvent e = captured(com.l299l.newbedwars.api.events.TrapTriggerEvent.class);
        assertEquals("BLINDTRAP", e.getTrap());
        assertEquals("Red", e.getTeam().getName());
        assertSame(player, e.getEnemy());
    }

    @Test
    void respawnCarriesPlayerAndTeam() {
        ApiEvents.respawn(arena, player, team);
        com.l299l.newbedwars.api.events.PlayerRespawnArenaEvent e = captured(com.l299l.newbedwars.api.events.PlayerRespawnArenaEvent.class);
        assertSame(player, e.getPlayer());
        assertEquals("Red", e.getTeam().getName());
    }

    @Test
    void partyChangeShowsTheLiveParty() {
        java.util.UUID leader = java.util.UUID.randomUUID();
        com.l299l.newbedwars.parties.Party party = new com.l299l.newbedwars.parties.Party(leader);
        ApiEvents.partyChange(null, party, leader, com.l299l.newbedwars.api.events.PartyChangeEvent.Change.CREATED);
        com.l299l.newbedwars.api.events.PartyChangeEvent e = captured(com.l299l.newbedwars.api.events.PartyChangeEvent.class);
        assertEquals(leader, e.getParty().getLeader());
        assertTrue(e.getParty().contains(leader));
        assertEquals(1, e.getParty().size());
        assertEquals(com.l299l.newbedwars.api.events.PartyChangeEvent.Change.CREATED, e.getChange());
    }

    @Test
    void eventsAreSkippedWithoutAServer() {
        bukkit.when(Bukkit::getPluginManager).thenThrow(new NullPointerException());
        assertEquals(4, ApiEvents.shopPurchase(arena, player, "Wool", ShopPurchaseEvent.Kind.ITEM, "IRON", 4, null));
    }

    @Test
    void shopItemStackCanBeReplaced() {
        org.bukkit.inventory.ItemStack oak = mock(org.bukkit.inventory.ItemStack.class);
        org.bukkit.inventory.ItemStack mangrove = mock(org.bukkit.inventory.ItemStack.class);
        when(mangrove.getType()).thenReturn(org.bukkit.Material.STONE);
        doAnswer(inv -> {
            if (inv.getArgument(0) instanceof com.l299l.newbedwars.api.events.ShopItemStackEvent e) {
                assertEquals("Planks", e.getItemName());
                assertEquals(com.l299l.newbedwars.api.events.ShopItemStackEvent.Use.ICON, e.getUse());
                assertTrue(e.getArena().isEmpty());
                e.setItem(mangrove);
            }
            return null;
        }).when(pm).callEvent(any(Event.class));
        assertSame(mangrove, ApiEvents.shopItemStack(player, "Planks", com.l299l.newbedwars.api.events.ShopItemStackEvent.Use.ICON, oak));
    }

    @Test
    void shopItemStackRefusesAir() {
        com.l299l.newbedwars.api.events.ShopItemStackEvent e = new com.l299l.newbedwars.api.events.ShopItemStackEvent(
                player, "Planks", com.l299l.newbedwars.api.events.ShopItemStackEvent.Use.GIVEN, mock(org.bukkit.inventory.ItemStack.class), null, null);
        org.bukkit.inventory.ItemStack air = mock(org.bukkit.inventory.ItemStack.class);
        when(air.getType()).thenReturn(org.bukkit.Material.AIR);
        assertThrows(IllegalArgumentException.class, () -> e.setItem(air));
        assertThrows(IllegalArgumentException.class, () -> e.setItem(null));
    }
}
