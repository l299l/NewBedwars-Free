package com.l299l.newbedwars.events.lifecycle;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrivateChatTest {

    private Messages messages;
    private ChatEvent listener;
    private IArena arena;
    private Gamerules rules;
    private ArrayList<Player> spectators;
    private Player inGame;
    private Player outside;
    private World arenaWorld;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        messages = mock(Messages.class);
        when(plugin.getMessages()).thenReturn(messages);
        listener = new ChatEvent(plugin);

        arenaWorld = mock(World.class);
        arena = mock(IArena.class);
        rules = mock(Gamerules.class);
        rules.AllowPrivateChat = false;
        spectators = new ArrayList<>();
        when(arena.getGamerules()).thenReturn(rules);
        when(arena.status()).thenReturn(GameStatus.playing);
        when(arena.getSpectators()).thenReturn(spectators);
        ArenaRegistry.bindWorld(arenaWorld, arena);

        inGame = mock(Player.class);
        when(inGame.getWorld()).thenReturn(arenaWorld);
        when(arena.isPlayerInArena(inGame)).thenReturn(true);

        outside = mock(Player.class);
        when(outside.getWorld()).thenReturn(mock(World.class));
    }

    @AfterEach
    void tearDown() {
        ArenaRegistry.clear();
    }

    private PlayerCommandPreprocessEvent command(Player sender, String message) {
        PlayerCommandPreprocessEvent event = mock(PlayerCommandPreprocessEvent.class);
        when(event.getPlayer()).thenReturn(sender);
        when(event.getMessage()).thenReturn(message);
        return event;
    }

    private void run(PlayerCommandPreprocessEvent event) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Ingame")).thenReturn(inGame);
            bukkit.when(() -> Bukkit.getPlayerExact("Outside")).thenReturn(outside);
            listener.onPrivateMessage(event);
        }
    }

    @Test
    void commandNamesIgnoreCaseAndNamespace() {
        assertEquals("msg", ChatEvent.commandName("MSG"));
        assertEquals("tell", ChatEvent.commandName("minecraft:tell"));
        assertEquals("r", ChatEvent.commandName("essentials:r"));
    }

    @Test
    void playerInARunningGameCanNotSendPrivateMessages() {
        PlayerCommandPreprocessEvent event = command(inGame, "/msg Outside hi");
        run(event);
        verify(event).setCancelled(true);
        verify(messages).send(inGame, "PrivateChatDisabled");
    }

    @Test
    void replyAndNamespacedCommandsAreBlockedToo() {
        PlayerCommandPreprocessEvent reply = command(inGame, "/r hi");
        run(reply);
        verify(reply).setCancelled(true);
        PlayerCommandPreprocessEvent namespaced = command(inGame, "/minecraft:tell Outside hi");
        run(namespaced);
        verify(namespaced).setCancelled(true);
    }

    @Test
    void messagesToAPlayerInARunningGameAreBlocked() {
        PlayerCommandPreprocessEvent event = command(outside, "/w Ingame hi");
        run(event);
        verify(event).setCancelled(true);
        verify(messages).send(outside, "PrivateChatDisabled");
    }

    @Test
    void spectatorsOfTheArenaAreBlocked() {
        Player spectator = mock(Player.class);
        when(spectator.getWorld()).thenReturn(arenaWorld);
        spectators.add(spectator);
        PlayerCommandPreprocessEvent event = command(spectator, "/msg Outside hi");
        run(event);
        verify(event).setCancelled(true);
    }

    @Test
    void allowedWhenTheGameruleIsOn() {
        rules.AllowPrivateChat = true;
        PlayerCommandPreprocessEvent event = command(inGame, "/msg Outside hi");
        run(event);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    void allowedWhileTheArenaIsWaiting() {
        when(arena.status()).thenReturn(GameStatus.waiting);
        PlayerCommandPreprocessEvent event = command(inGame, "/msg Outside hi");
        run(event);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    void adminsBypassTheRule() {
        when(inGame.hasPermission(ChatEvent.PRIVATE_CHAT_BYPASS)).thenReturn(true);
        PlayerCommandPreprocessEvent event = command(inGame, "/msg Outside hi");
        run(event);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    void otherCommandsAndPlayersOutsideGamesAreUntouched() {
        PlayerCommandPreprocessEvent other = command(inGame, "/bw leave");
        run(other);
        verify(other, never()).setCancelled(anyBoolean());
        PlayerCommandPreprocessEvent lobby = command(outside, "/msg Outside hi");
        run(lobby);
        verify(lobby, never()).setCancelled(anyBoolean());
    }
}
