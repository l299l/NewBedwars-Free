package com.l299l.newbedwars.parties;

import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartyManagerTest {
    private Messages messages;
    private PartyManager pm;

    @BeforeEach
    void setUp() {
        Plugin plugin = mock(Plugin.class);
        messages = mock(Messages.class);
        pm = spy(new PartyManager(plugin, messages));
        doNothing().when(pm).scheduleInviteExpiry(any(), any(), any());
    }

    private Player mockPlayer(String name) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getName()).thenReturn(name);
        when(p.isOnline()).thenReturn(true);
        return p;
    }

    @Test
    void createPartyAddsPlayerToMap() {
        Player p = mockPlayer("Alice");
        Party party = pm.createParty(p);
        assertTrue(pm.isPlayerInParty(p));
        assertEquals(party, pm.getParty(p));
    }

    @Test
    void hasPendingInviteFalseBeforeInvite() {
        Player p = mockPlayer("Bob");
        assertFalse(pm.hasPendingInvite(p));
    }

    @Test
    void sendInviteRegistersPendingInvite() {
        Player inviter = mockPlayer("Alice");
        Player invitee = mockPlayer("Bob");
        Party party = pm.createParty(inviter);
        pm.sendInvite(inviter, invitee, party);
        assertTrue(pm.hasPendingInvite(invitee));
        assertEquals(party, pm.getPendingInvite(invitee));
    }

    @Test
    void acceptInviteAddsPlayerToPartyAndClearsPending() {
        Player inviter = mockPlayer("Alice");
        Player invitee = mockPlayer("Bob");
        Party party = pm.createParty(inviter);
        pm.sendInvite(inviter, invitee, party);
        assertTrue(pm.acceptInvite(invitee));
        assertTrue(pm.isPlayerInParty(invitee));
        assertFalse(pm.hasPendingInvite(invitee));
        assertEquals(party, pm.getParty(invitee));
    }

    @Test
    void acceptInviteReturnsFalseWithNoPendingInvite() {
        Player p = mockPlayer("Carol");
        assertFalse(pm.acceptInvite(p));
    }

    @Test
    void declineInviteRemovesPendingInvite() {
        Player inviter = mockPlayer("Alice");
        Player invitee = mockPlayer("Bob");
        Party party = pm.createParty(inviter);
        pm.sendInvite(inviter, invitee, party);
        pm.declineInvite(invitee);
        assertFalse(pm.hasPendingInvite(invitee));
        assertFalse(pm.isPlayerInParty(invitee));
    }

    @Test
    void leavePartyRemovesNonAdminMember() {
        Player admin = mockPlayer("Alice");
        Player member = mockPlayer("Bob");
        Party party = pm.createParty(admin);
        pm.sendInvite(admin, member, party);
        pm.acceptInvite(member);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(admin.getUniqueId())).thenReturn(admin);
            pm.leaveParty(member);
        }
        assertFalse(pm.isPlayerInParty(member));
        assertTrue(pm.isPlayerInParty(admin));
    }

    @Test
    void leavePartyTransfersAdminToNextMember() {
        Player admin = mockPlayer("Alice");
        Player member = mockPlayer("Bob");
        Party party = pm.createParty(admin);
        pm.sendInvite(admin, member, party);
        pm.acceptInvite(member);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(member.getUniqueId())).thenReturn(member);
            pm.leaveParty(admin);
        }
        assertFalse(pm.isPlayerInParty(admin));
        assertEquals(member.getUniqueId(), party.getAdmin());
    }

    @Test
    void disbandPartyRemovesAllMembers() {
        Player admin = mockPlayer("Alice");
        Player member = mockPlayer("Bob");
        Party party = pm.createParty(admin);
        pm.sendInvite(admin, member, party);
        pm.acceptInvite(member);
        pm.disbandParty(party);
        assertFalse(pm.isPlayerInParty(admin));
        assertFalse(pm.isPlayerInParty(member));
    }

    @Test
    void kickFromPartyRemovesTargetOnly() {
        Player admin = mockPlayer("Alice");
        Player member = mockPlayer("Bob");
        Party party = pm.createParty(admin);
        pm.sendInvite(admin, member, party);
        pm.acceptInvite(member);
        pm.kickFromParty(party, member.getUniqueId());
        assertFalse(pm.isPlayerInParty(member));
        assertTrue(pm.isPlayerInParty(admin));
    }

    @Test
    void transferAdminUpdatesAdminField() {
        Player admin = mockPlayer("Alice");
        Player member = mockPlayer("Bob");
        Party party = pm.createParty(admin);
        pm.sendInvite(admin, member, party);
        pm.acceptInvite(member);
        pm.transferAdmin(party, member.getUniqueId());
        assertEquals(member.getUniqueId(), party.getAdmin());
    }

    @Test
    void getOnlineMembersExcludesOfflinePlayers() {
        Player admin = mockPlayer("Alice");
        Player member = mockPlayer("Bob");
        Party party = pm.createParty(admin);
        pm.sendInvite(admin, member, party);
        pm.acceptInvite(member);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(admin.getUniqueId())).thenReturn(admin);
            bukkit.when(() -> Bukkit.getPlayer(member.getUniqueId())).thenReturn(null);
            List<Player> online = pm.getOnlineMembers(party);
            assertEquals(1, online.size());
            assertTrue(online.contains(admin));
        }
    }

    @Test
    void cleanupOfflinePlayerRemovesPendingInvite() {
        Player inviter = mockPlayer("Alice");
        Player invitee = mockPlayer("Bob");
        Party party = pm.createParty(inviter);
        pm.sendInvite(inviter, invitee, party);
        pm.cleanupOfflinePlayer(invitee);
        assertFalse(pm.hasPendingInvite(invitee));
    }
}
