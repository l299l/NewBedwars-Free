package com.l299l.newbedwars.panel.command;

import com.l299l.newbedwars.panel.NewBedwarsPanel;
import com.l299l.newbedwars.panel.PanelMessages;
import com.l299l.newbedwars.panel.network.PeerPins;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PanelCommandTest {
    private PeerPins pins;
    private UserCommands users;
    private PanelCommand command;

    @BeforeEach
    void setUp() {
        NewBedwarsPanel plugin = mock(NewBedwarsPanel.class);
        PanelMessages messages = mock(PanelMessages.class, invocation ->
                invocation.getMethod().getName().equals("get") ? invocation.getArgument(1) : "en");
        pins = mock(PeerPins.class);
        users = mock(UserCommands.class);
        when(plugin.messages()).thenReturn(messages);
        when(plugin.peerPins()).thenReturn(pins);
        when(plugin.userCommands()).thenReturn(users);
        command = new PanelCommand(plugin);
    }

    private <T extends CommandSender> T allowed(Class<T> type) {
        T sender = mock(type);
        when(sender.hasPermission(PanelCommand.PERMISSION)).thenReturn(true);
        return sender;
    }

    @Test
    void forgetNeedsOpForPlayers() {
        Player player = allowed(Player.class);
        when(player.isOp()).thenReturn(false);
        command.onCommand(player, null, "nbwpanel", new String[]{"network", "forget", "game1"});
        verify(pins, never()).remove(anyString());
        verify(player).sendMessage(contains("UserNeedsOp"));
    }

    @Test
    void forgetWorksForOpsAndConsole() {
        Player op = allowed(Player.class);
        when(op.isOp()).thenReturn(true);
        command.onCommand(op, null, "nbwpanel", new String[]{"network", "forget", "game1"});
        command.onCommand(allowed(ConsoleCommandSender.class), null, "nbwpanel", new String[]{"network", "forget", "game2"});
        verify(pins).remove("game1");
        verify(pins).remove("game2");
    }

    @Test
    void commandBlocksCanNotChangeAccountsOrPins() {
        BlockCommandSender block = allowed(BlockCommandSender.class);
        command.onCommand(block, null, "nbwpanel", new String[]{"network", "forget", "game1"});
        command.onCommand(block, null, "nbwpanel", new String[]{"user", "delete", "bob"});
        command.onCommand(block, null, "nbwpanel", new String[]{"user", "reset", "bob"});
        verify(pins, never()).remove(anyString());
        verify(users, never()).delete(anyString(), anyString());
        verify(users, never()).reset(anyString(), anyString());
    }
}
