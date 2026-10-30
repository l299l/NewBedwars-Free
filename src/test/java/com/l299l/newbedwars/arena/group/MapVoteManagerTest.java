package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MapVoteManagerTest {
    private MockedStatic<Bukkit> bukkit;
    private PluginManager pluginManager;
    private PartyManager parties;
    private ArenaGroupManager groups;
    private MapVoteManager votes;
    private IArena ended;
    private IArena ruins;
    private IArena desert;
    private Player alice;
    private Player bob;

    private IArena arena(String name, GameStatus status, int players) {
        IArena a = ArenaGroupTest.arena(name, 4, 1);
        when(a.isEnabled()).thenReturn(true);
        when(a.status()).thenReturn(status);
        when(a.getActivePlayerCount()).thenReturn(players);
        Gamerules rules = mock(Gamerules.class);
        rules.AllowParties = true;
        when(a.getGamerules()).thenReturn(rules);
        when(a.getSpectators()).thenReturn(new ArrayList<>());
        Arena.arenaByName.put(name, a);
        return a;
    }

    private Player player(String name) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getName()).thenReturn(name);
        when(p.isOnline()).thenReturn(true);
        when(p.getInventory()).thenReturn(mock(PlayerInventory.class));
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(mock(org.bukkit.inventory.Inventory.class));
        when(p.getOpenInventory()).thenReturn(view);
        return p;
    }

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        pluginManager = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        org.bukkit.inventory.ItemFactory items = mock(org.bukkit.inventory.ItemFactory.class);
        when(items.getItemMeta(any())).thenAnswer(inv -> mock(org.bukkit.inventory.meta.ItemMeta.class));
        bukkit.when(Bukkit::getItemFactory).thenReturn(items);
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        Messages messages = mock(Messages.class);
        when(messages.getMsg(any(Player.class), anyString())).thenReturn("x");
        when(plugin.getMessages()).thenReturn(messages);
        parties = mock(PartyManager.class);
        when(plugin.getPartyManager()).thenReturn(parties);
        Arena.arenaByName.clear();

        ended = arena("castle", GameStatus.ending, 2);
        ruins = arena("ruins", GameStatus.waiting, 0);
        desert = arena("desert", GameStatus.waiting, 0);
        alice = player("Alice");
        bob = player("Bob");
        ended.getSpectators().addAll(List.of(alice, bob));

        groups = new ArenaGroupManager();
        ArenaGroup solo = new ArenaGroup("solo", null);
        solo.add("castle");
        solo.add("ruins");
        solo.add("desert");
        groups.register(solo);
        votes = new MapVoteManager();
    }

    @AfterEach
    void tearDown() {
        new ArenaGroupManager();
        Arena.arenaByName.clear();
        NewBedwars.plugin = null;
        bukkit.close();
    }

    @Test
    void voteOpensWhenTheGameEndsAndGivesItems() {
        assertFalse(votes.isOpen(ended));
        MapVoteManager.gameEnded(ended);
        assertTrue(votes.isOpen(ended));
        verify(alice.getInventory()).setItem(eq(2), any());
        verify(bob.getInventory()).setItem(eq(2), any());
        verify(ended).broadcast("MapVoteOpen");
    }

    @Test
    void optionsAreOtherFreeGroupArenasNotTheEndedOne() {
        IArena running = arena("lake", GameStatus.playing, 0);
        IArena full = arena("tower", GameStatus.waiting, 3);   // 1 free slot, 2 players move
        IArena off = arena("ice", GameStatus.waiting, 0);
        when(off.isEnabled()).thenReturn(false);
        for (String n : List.of("lake", "tower", "ice")) groups.group("solo").add(n);
        assertEquals(List.of(ruins, desert), votes.options(ended));
    }

    @Test
    void closeMovesEveryoneToTheWinnerAfterStop() {
        MapVoteManager.gameEnded(ended);
        votes.castVote(alice, ended, desert);
        assertEquals(1, votes.vote(ended).votes("desert"));
        when(desert.join(any())).thenReturn(true);

        Runnable move = MapVoteManager.closeEndVote(ended);
        assertNotNull(move);
        verify(desert, never()).join(any());
        move.run();
        verify(desert).join(alice);
        verify(desert).join(bob);
        assertFalse(votes.isOpen(ended));
    }

    @Test
    void partiesMoveTogether() {
        Party party = mock(Party.class);
        when(parties.getParty(alice)).thenReturn(party);
        when(parties.getParty(bob)).thenReturn(party);
        MapVoteManager.gameEnded(ended);
        votes.castVote(alice, ended, ruins);
        when(ruins.joinParty(any())).thenReturn(true);

        MapVoteManager.closeEndVote(ended).run();

        verify(ruins).joinParty(List.of(alice, bob));
        verify(ruins, never()).join(any());
    }

    @Test
    void noFreeArenaSendsEveryoneToTheLobby() {
        when(ruins.status()).thenReturn(GameStatus.playing);
        when(desert.status()).thenReturn(GameStatus.restarting);
        MapVoteManager.gameEnded(ended);
        assertNull(MapVoteManager.closeEndVote(ended));
        verify(ended).broadcast("MapVoteNoArena");
    }

    @Test
    void notGroupedOrGameruleOffMeansNoVote() {
        ended.getGamerules().MapVote = false;
        MapVoteManager.gameEnded(ended);
        assertFalse(votes.isOpen(ended));
        assertNull(MapVoteManager.closeEndVote(ended));
        verify(alice.getInventory(), never()).setItem(anyInt(), any());
    }

    @Test
    void votingNeedsTheEndPhase() {
        MapVoteManager.gameEnded(ended);
        when(ended.status()).thenReturn(GameStatus.restarting);
        votes.castVote(alice, ended, ruins);
        assertEquals(0, votes.vote(ended).votes("ruins"));
    }

    @Test
    void cancelledVoteEventIsIgnored() {
        MapVoteManager.gameEnded(ended);
        doAnswer(inv -> {
            Event e = inv.getArgument(0);
            if (e instanceof Cancellable c) c.setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any(Event.class));
        votes.castVote(alice, ended, ruins);
        assertEquals(0, votes.vote(ended).votes("ruins"));
    }

    @Test
    void leavingDropsTheVote() {
        MapVoteManager.gameEnded(ended);
        votes.castVote(alice, ended, ruins);
        MapVoteManager.playerLeft(ended, alice);
        assertEquals(0, votes.vote(ended).votes("ruins"));
    }

    @Test
    void itemSlotAvoidsSpectatorItems() {
        Integer old = com.l299l.newbedwars.config.properties.Properties.MapVoteItemSlot;
        try {
            for (int taken : new int[]{0, 4, 8, -1, 9}) {
                com.l299l.newbedwars.config.properties.Properties.MapVoteItemSlot = taken;
                assertEquals(2, MapVoteManager.itemSlot(), "slot " + taken);
            }
            com.l299l.newbedwars.config.properties.Properties.MapVoteItemSlot = 6;
            assertEquals(6, MapVoteManager.itemSlot());
        } finally {
            com.l299l.newbedwars.config.properties.Properties.MapVoteItemSlot = old;
        }
    }

    @Test
    void someoneListedTwiceMovesOnce() {
        ended.getSpectators().add(alice);
        MapVoteManager.gameEnded(ended);
        votes.castVote(alice, ended, ruins);
        when(ruins.join(any())).thenReturn(true);
        MapVoteManager.closeEndVote(ended).run();
        verify(ruins, times(1)).join(alice);
    }
}
