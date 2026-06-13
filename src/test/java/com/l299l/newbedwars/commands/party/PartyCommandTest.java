package com.l299l.newbedwars.commands.party;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PartyCommandTest {
    private Messages mockMessages;
    private PartyManager pm;
    private PartyCommand cmd;
    private Player p1, p2;

    @BeforeEach
    void setUp() {
        mockMessages = mock(Messages.class);
        Plugin plugin = mock(Plugin.class);

        pm = new PartyManager(plugin, mockMessages) {
            @Override
            protected void scheduleInviteExpiry(Player inviter, Player invitee, Party party) {
                // no-op: skip BukkitRunnable scheduling in tests
            }
        };

        NewBedwars mockPlugin = mock(NewBedwars.class);
        NewBedwars.plugin = mockPlugin;
        when(mockPlugin.getMessages()).thenReturn(mockMessages);
        when(mockPlugin.getPartyManager()).thenReturn(pm);

        p1 = mockPlayer("Alice");
        p2 = mockPlayer("Bob");

        cmd = new PartyCommand();
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    private Player mockPlayer(String name) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getName()).thenReturn(name);
        when(p.hasPermission("newbedwars.party")).thenReturn(true);
        return p;
    }

    @Test
    void noArgs_outputsHelpLines() {
        cmd.onCommand(p1, null, "party", new String[]{});
        verify(p1, atLeastOnce()).sendMessage(anyString());
    }

    @Test
    void inviteSelf_sendsError() {
        cmd.onCommand(p1, null, "party", new String[]{"invite", "Alice"});
        verify(mockMessages).send(p1, "PartyCantInviteSelf");
    }

    @Test
    void inviteOfflinePlayer_sendsPlayerNotFound() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Nobody")).thenReturn(null);
            cmd.onCommand(p1, null, "party", new String[]{"invite", "Nobody"});
        }
        verify(mockMessages).send(eq(p1), eq("PartyPlayerNotFound"), any());
    }

    @Test
    void inviteOnlinePlayer_createsPartyAndRegistersInvite() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Bob")).thenReturn(p2);
            cmd.onCommand(p1, null, "party", new String[]{"invite", "Bob"});
        }
        verify(mockMessages).send(eq(p1), eq("PartyInviteSent"), any());
        verify(mockMessages).send(eq(p2), eq("PartyInviteReceived"), any());
        assertTrue(pm.hasPendingInvite(p2));
        assertTrue(pm.isPlayerInParty(p1));
    }

    @Test
    void acceptWithNoPendingInvite_sendsError() {
        cmd.onCommand(p2, null, "party", new String[]{"accept"});
        verify(mockMessages).send(p2, "PartyNoPendingInvite");
    }

    @Test
    void acceptInvite_addsToPartyAndNotifiesMembers() {
        Party party = pm.createParty(p1);
        pm.sendInvite(p1, p2, party);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(p1.getUniqueId())).thenReturn(p1);
            cmd.onCommand(p2, null, "party", new String[]{"accept"});
        }

        verify(mockMessages).send(eq(p2), eq("PartyAccepted"), any());
        verify(mockMessages).send(eq(p1), eq("PartyMemberJoined"), any());
        assertTrue(pm.isPlayerInParty(p2));
    }

    @Test
    void denyWithNoPendingInvite_sendsError() {
        cmd.onCommand(p2, null, "party", new String[]{"deny"});
        verify(mockMessages).send(p2, "PartyNoPendingInvite");
    }

    @Test
    void denyInvite_removesInviteAndNotifiesAdmin() {
        Party party = pm.createParty(p1);
        pm.sendInvite(p1, p2, party);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(p1.getUniqueId())).thenReturn(p1);
            cmd.onCommand(p2, null, "party", new String[]{"deny"});
        }

        assertFalse(pm.hasPendingInvite(p2));
        assertFalse(pm.isPlayerInParty(p2));
        verify(mockMessages).send(eq(p2), eq("PartyDenied"), any());
        verify(mockMessages).send(eq(p1), eq("PartyMemberDenied"), any());
    }

    @Test
    void leaveWithNoParty_sendsError() {
        cmd.onCommand(p1, null, "party", new String[]{"leave"});
        verify(mockMessages).send(p1, "PartyNoParty");
    }

    @Test
    void kickAsNonAdmin_sendsNotAdminError() {
        Party party = pm.createParty(p1);
        pm.sendInvite(p1, p2, party);
        pm.acceptInvite(p2);
        // p2 is a member but not admin
        cmd.onCommand(p2, null, "party", new String[]{"kick", "Alice"});
        verify(mockMessages).send(p2, "PartyNotAdmin");
    }

    @Test
    void adminTransferAsNonAdmin_sendsNotAdminError() {
        Party party = pm.createParty(p1);
        pm.sendInvite(p1, p2, party);
        pm.acceptInvite(p2);
        cmd.onCommand(p2, null, "party", new String[]{"admin", "Alice"});
        verify(mockMessages).send(p2, "PartyNotAdmin");
    }
}
