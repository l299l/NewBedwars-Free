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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            new ChatEvent(plugin).onChat(event);
        }

        verify(spectator).sendMessage("\u00a77Name: \u00a7floud");
        assertTrue(event.isCancelled());
    }
}
