package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JoinCommandTest {
    private Messages mockMessages;
    private PartyManager mockPm;
    private Player player;
    private JoinCommand cmd;

    @BeforeEach
    void setUp() {
        mockMessages = mock(Messages.class);
        mockPm = mock(PartyManager.class);
        NewBedwars mockPlugin = mock(NewBedwars.class);
        NewBedwars.plugin = mockPlugin;
        when(mockPlugin.getMessages()).thenReturn(mockMessages);
        when(mockPlugin.getPartyManager()).thenReturn(mockPm);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("TestPlayer");
        when(player.isOp()).thenReturn(false);
        when(player.getWorld()).thenReturn(null);

        when(mockPm.getParty(player)).thenReturn(null);

        Properties.RequireJoinPermission = false;
        Arena.arenaByName.clear();
        Arena.arenaByWorld.clear();

        cmd = new JoinCommand();
    }

    @AfterEach
    void tearDown() {
        Arena.arenaByName.clear();
        Arena.arenaByWorld.clear();
        NewBedwars.plugin = null;
    }

    @Test
    void unknownArena_sendsArenaNotExists() {
        cmd.perform(player, new String[]{"join", "ghost"}, null);
        verify(mockMessages).send(player, "ArenaNotExists");
    }

    @Test
    void arenaInPlayingStatus_sendsRunningError() {
        IArena arena = mockArena(GameStatus.playing);
        Arena.arenaByName.put("arena1", arena);
        cmd.perform(player, new String[]{"join", "arena1"}, null);
        verify(mockMessages).send(player, "ArenaRunningError");
    }

    @Test
    void arenaInRestartingStatus_sendsRestartingError() {
        IArena arena = mockArena(GameStatus.restarting);
        Arena.arenaByName.put("arena1", arena);
        cmd.perform(player, new String[]{"join", "arena1"}, null);
        verify(mockMessages).send(player, "ArenaRestartingError");
    }

    @Test
    void arenaInWaitingStatus_callsJoin() {
        IArena arena = mockArena(GameStatus.waiting);
        when(arena.join(player)).thenReturn(true);
        Arena.arenaByName.put("arena1", arena);
        cmd.perform(player, new String[]{"join", "arena1"}, null);
        verify(arena).join(player);
    }

    @Test
    void arenaFull_sendsIsFullError() {
        IArena arena = mockArena(GameStatus.waiting);
        when(arena.join(player)).thenReturn(false);
        Arena.arenaByName.put("arena1", arena);
        cmd.perform(player, new String[]{"join", "arena1"}, null);
        verify(mockMessages).send(player, "ArenaIsFullError");
    }

    @Test
    void partyNonAdminMember_sendsOnlyAdminCanJoin() {
        Party party = mock(Party.class);
        UUID adminId = UUID.randomUUID();
        when(party.isAdmin(player.getUniqueId())).thenReturn(false);
        when(party.getAdmin()).thenReturn(adminId);
        when(mockPm.getParty(player)).thenReturn(party);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(adminId)).thenReturn(null);
            cmd.perform(player, new String[]{"join", "arena1"}, null);
        }
        verify(mockMessages).send(eq(player), eq("PartyOnlyAdminCanJoin"), any());
    }

    @Test
    void disabledArena_sendsNotEnabledMessage() {
        IArena arena = mock(IArena.class);
        when(arena.isEnabled()).thenReturn(false);
        when(arena.isPlayerInArena(player)).thenReturn(false);
        when(arena.getArenaName()).thenReturn("arena1");
        Arena.arenaByName.put("arena1", arena);
        cmd.perform(player, new String[]{"join", "arena1"}, null);
        verify(mockMessages).send(eq(player), eq("ArenaNotEnabled"), any());
    }

    private IArena mockArena(GameStatus status) {
        IArena arena = mock(IArena.class);
        when(arena.isEnabled()).thenReturn(true);
        when(arena.status()).thenReturn(status);
        when(arena.isPlayerInArena(player)).thenReturn(false);
        return arena;
    }
}
