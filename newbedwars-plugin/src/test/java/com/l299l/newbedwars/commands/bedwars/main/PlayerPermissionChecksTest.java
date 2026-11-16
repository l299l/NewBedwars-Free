package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PlayerPermissionChecksTest {
    private Messages messages;
    private Player player;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getPartyManager()).thenReturn(mock(PartyManager.class));
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(anyString())).thenReturn(false);
        when(player.isOp()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    private void assertDenied(SubCommand cmd, String... args) {
        cmd.perform(player, args, null);
        verify(messages).send(player, "NoPermissions");
        verifyNoMoreInteractions(messages);
    }

    @Test
    void rejoinNeedsPermission() {
        assertDenied(new RejoinCommand(), "rejoin");
        verify(player).hasPermission("newbedwars.bw.rejoin");
    }

    @Test
    void spectateNeedsPermission() {
        assertDenied(new SpectateCommand(), "spectate", "castle");
        verify(player).hasPermission("newbedwars.bw.spectate");
    }

    @Test
    void langNeedsPermission() {
        assertDenied(new LangCommand(), "lang", "pl");
        verify(player).hasPermission("newbedwars.bw.lang");
    }

    @Test
    void helpNeedsPermission() {
        assertDenied(new HelpCommand(p -> Map.of()), "help");
        verify(player).hasPermission("newbedwars.bw.help");
    }

    @Test
    void lobbyNeedsPermission() {
        assertDenied(new LobbyCommand(), "lobby");
        verify(player).hasPermission("newbedwars.bw.lobby");
    }

    @Test
    void joinNeedsPermission() {
        assertFalse(ArenaJoinService.mayJoin(player));
        verify(messages).send(player, "NoPermissions");
        verify(player).hasPermission("newbedwars.bw.join");
    }

    @Test
    void opBypassesTheCheck() {
        when(player.isOp()).thenReturn(true);
        when(messages.getMsg(any(Player.class), anyString())).thenReturn("");
        new HelpCommand(p -> Map.of()).perform(player, new String[]{"help"}, null);
        verify(messages, never()).send(player, "NoPermissions");
    }
}
