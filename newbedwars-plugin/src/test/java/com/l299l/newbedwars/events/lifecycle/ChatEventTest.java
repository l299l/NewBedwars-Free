package com.l299l.newbedwars.events.lifecycle;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import com.l299l.newbedwars.api.events.ArenaChatEvent;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatEventTest {

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
    }

    @Test
    void colorCodesInTheMessageAreStrippedWithoutPermission() {
        String out = ChatEvent.format("&7[A] &fPlayer &8» &f/message/", "&cred &lbold \u00a7a", false);
        assertEquals("\u00a77[A] \u00a7fPlayer \u00a78» \u00a7fred bold ", out);
    }

    @Test
    void colorCodesInTheMessageWorkWithPermission() {
        String out = ChatEvent.format("&7: /message/", "&chi", true);
        assertEquals("\u00a77: \u00a7chi", out);
    }

    @Test
    void messageIsInsertedLiterally() {
        assertEquals("\u00a77$1 \\ /player/", ChatEvent.format("&7/message/", "$1 \\ /player/", false));
    }

    private NewBedwars plugin;
    private Messages messages;
    private Player sender;
    private World world;
    private IArena arena;

    private void arenaSetup() {
        plugin = mock(NewBedwars.class);
        messages = mock(Messages.class);
        when(plugin.getMessages()).thenReturn(messages);
        sender = mock(Player.class);
        world = mock(World.class);
        when(sender.getWorld()).thenReturn(world);
        arena = mock(IArena.class);
        ArenaRegistry.bindWorld(world, arena);
    }

    @Test
    void cancelledEventIsIgnored() {
        arenaSetup();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, sender, "hi", new HashSet<>());
        event.setCancelled(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            new ChatEvent(plugin).onChat(event);
        }

        verifyNoInteractions(messages, arena);
        assertTrue(event.isCancelled());
    }

    @Test
    void asyncArenaChatIsCancelledAtOnceAndHandledLaterOnTheMainThread() {
        arenaSetup();
        Player lobbyPlayer = mock(Player.class);
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, sender, "hi", new HashSet<>(Set.of(lobbyPlayer)));
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            new ChatEvent(plugin).onChat(event);
        }

        assertTrue(event.isCancelled());
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
        verify(scheduler, never()).callSyncMethod(any(), any());
        verifyNoInteractions(arena, lobbyPlayer);
    }

    @Test
    void asyncLobbyChatHidesArenaPlayersWithoutTheMainThread() {
        arenaSetup();
        Player lobbySender = mock(Player.class);
        World lobby = mock(World.class);
        when(lobbySender.getWorld()).thenReturn(lobby);
        Player lobbyPlayer = mock(Player.class);
        when(lobbyPlayer.getWorld()).thenReturn(lobby);
        Player inArena = mock(Player.class);
        when(inArena.getWorld()).thenReturn(world);
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, lobbySender, "hi",
                new HashSet<>(Set.of(lobbyPlayer, inArena)));
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            new ChatEvent(plugin).onChat(event);
        }

        assertFalse(event.isCancelled());
        assertEquals(Set.of(lobbyPlayer), event.getRecipients());
        verifyNoInteractions(scheduler);
    }

    @Test
    void asyncChatDuringShutdownIsDroppedQuietly() {
        arenaSetup();
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, sender, "hi", new HashSet<>());
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(plugin), any(Runnable.class)))
                .thenThrow(new org.bukkit.plugin.IllegalPluginAccessException("disabled"));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            assertDoesNotThrow(() -> new ChatEvent(plugin).onChat(event));
        }
        assertTrue(event.isCancelled());
    }

    @Test
    void arenaWorldSnapshotFollowsTheRegistry() {
        World w = mock(World.class);
        assertFalse(ArenaRegistry.isArenaWorld(w));
        ArenaRegistry.bindWorld(w, mock(IArena.class));
        assertTrue(ArenaRegistry.isArenaWorld(w));
        ArenaRegistry.unbindWorld(w);
        assertFalse(ArenaRegistry.isArenaWorld(w));
        ArenaRegistry.bindWorld(w, mock(IArena.class));
        ArenaRegistry.unregister("x", w);
        assertFalse(ArenaRegistry.isArenaWorld(w));
        assertFalse(ArenaRegistry.isArenaWorld(null));
    }

    @Test
    void arenaChatStripsColorsOfPlayersWithoutPermission() {
        arenaSetup();
        Properties.ArenaChatEnabled = true;
        Gamerules rules = mock(Gamerules.class);
        rules.AllowGlobalChat = true;
        rules.AllowTeamChat = true;
        when(arena.getGamerules()).thenReturn(rules);
        when(arena.coloredName(sender)).thenReturn("Name");
        Player spectator = mock(Player.class);
        when(arena.getSpectators()).thenReturn(new ArrayList<>(List.of(spectator)));
        when(arena.getPlayers()).thenReturn(Set.of());
        when(messages.getMsg(sender, "ArenaChatFormat")).thenReturn("&7/player/: &f/message/");
        when(sender.hasPermission("newbedwars.chat.color")).thenReturn(false);

        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, sender, "&cloud", new HashSet<>());
        PluginManager pm = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
            new ChatEvent(plugin).onChat(event);
        }

        verify(spectator).sendMessage("\u00a77Name: \u00a7floud");
        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> fired = ArgumentCaptor.forClass(Event.class);
        verify(pm).callEvent(fired.capture());
        ArenaChatEvent chat = assertInstanceOf(ArenaChatEvent.class, fired.getValue());
        assertEquals("loud", chat.getMessage());
        assertFalse(chat.isTeamChat());
        assertSame(sender, chat.getPlayer());
    }

    private void chat(String text, boolean teamByDefault, Player teammate, Player other) {
        arenaSetup();
        Properties.ArenaChatEnabled = true;
        Gamerules rules = mock(Gamerules.class);
        rules.AllowGlobalChat = true;
        rules.AllowTeamChat = true;
        rules.TeamChatByDefault = teamByDefault;
        when(arena.getGamerules()).thenReturn(rules);
        when(arena.coloredName(sender)).thenReturn("Name");
        com.l299l.newbedwars.arena.team.Team team = mock(com.l299l.newbedwars.arena.team.Team.class);
        when(team.colored(any())).thenAnswer(i -> i.getArgument(0));
        when(team.getDisplayName()).thenReturn("Red");
        when(team.getPlayers()).thenReturn(new ArrayList<>(List.of(sender, teammate)));
        when(arena.getTeam(sender)).thenReturn(team);
        when(arena.getSpectators()).thenReturn(new ArrayList<>(List.of(other)));
        when(arena.getPlayers()).thenReturn(Set.of());
        when(messages.getMsg(sender, "ArenaChatFormat")).thenReturn("G:/message/");
        when(messages.getMsg(sender, "TeamChatFormat")).thenReturn("T:/message/");
        when(sender.getName()).thenReturn("Steve");
        when(sender.hasPermission("newbedwars.chat.color")).thenReturn(false);
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, sender, text, new HashSet<>());
        PluginManager pm = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
            new ChatEvent(plugin).onChat(event);
        }
    }

    @Test
    void teamDefaultSendsPlainMessagesToTheTeamOnly() {
        Player teammate = mock(Player.class);
        Player other = mock(Player.class);
        chat("hi", true, teammate, other);
        verify(teammate).sendMessage("T:hi");
        verify(other, never()).sendMessage(anyString());
    }

    @Test
    void teamDefaultSendsBangMessagesToEveryone() {
        Player teammate = mock(Player.class);
        Player other = mock(Player.class);
        chat("!hi", true, teammate, other);
        verify(other).sendMessage("G:hi");
        verify(teammate, never()).sendMessage(anyString());
    }

    @Test
    void globalDefaultStillSendsBangMessagesToTheTeam() {
        Player teammate = mock(Player.class);
        Player other = mock(Player.class);
        chat("!hi", false, teammate, other);
        verify(teammate).sendMessage("T:hi");
        verify(other, never()).sendMessage(anyString());
    }

    @Test
    void lobbyJoinMessagesCanBeSwitchedOff() {
        Properties.LobbyJoinMessages = false;
        try {
            assertFalse(ChatEvent.showServerJoinMessages());
        } finally {
            Properties.LobbyJoinMessages = true;
        }
    }

    @Test
    void lobbyJoinMessagesAreOnByDefault() {
        assertTrue(ChatEvent.showServerJoinMessages());
    }
}
