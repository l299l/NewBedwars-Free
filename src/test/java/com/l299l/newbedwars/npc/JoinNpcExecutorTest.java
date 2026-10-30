package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
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

import java.util.List;
import java.util.UUID;

import static com.l299l.newbedwars.npc.NpcTestArenas.arena;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JoinNpcExecutorTest {
    private Messages messages;
    private PartyManager pm;
    private Player player;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        pm = mock(PartyManager.class);
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getPartyManager()).thenReturn(pm);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Properties.RequireJoinPermission = false;
        Arena.arenaByName.clear();
        Arena.arenaByWorld.clear();
    }

    @AfterEach
    void tearDown() {
        Arena.arenaByName.clear();
        Arena.arenaByWorld.clear();
        NewBedwars.plugin = null;
    }

    private void put(IArena arena) {
        Arena.arenaByName.put(arena.getArenaName(), arena);
    }

    @Test
    void group_joinsFullestArenaWithRoom() {
        IArena empty = arena("solo_1", GameStatus.waiting, 4, 1, 0);
        IArena fuller = arena("solo_2", GameStatus.waiting, 4, 1, 2);
        IArena full = arena("solo_3", GameStatus.waiting, 4, 1, 4);
        IArena running = arena("solo_4", GameStatus.playing, 4, 1, 3);
        IArena otherGroup = arena("duo_1", GameStatus.waiting, 4, 2, 7);
        for (IArena a : List.of(empty, fuller, full, running, otherGroup)) put(a);
        when(fuller.join(player)).thenReturn(true);

        JoinNpcExecutor.perform(player, JoinNpcAction.parse("GROUP", "solo_*"));

        verify(fuller).join(player);
        verify(empty, never()).join(any());
        verify(otherGroup, never()).join(any());
    }

    @Test
    void group_noFreeArena_sendsMessage() {
        put(arena("solo_1", GameStatus.playing, 4, 1, 4));
        JoinNpcExecutor.perform(player, JoinNpcAction.parse("GROUP", "solo_*"));
        verify(messages).send(eq(player), eq("NpcNoFreeArena"), any());
    }

    @Test
    void group_partyNeedsRoomForEveryone() {
        Party party = mock(Party.class);
        when(party.isAdmin(player.getUniqueId())).thenReturn(true);
        when(pm.getParty(player)).thenReturn(party);
        List<Player> members = List.of(player, mock(Player.class));
        when(pm.getOnlineMembers(party)).thenReturn(members);
        Gamerules rules = mock(Gamerules.class);
        rules.AllowParties = true;

        IArena almostFull = arena("duo_1", GameStatus.waiting, 2, 2, 3);
        IArena roomy = arena("duo_2", GameStatus.waiting, 2, 2, 1);
        when(almostFull.getGamerules()).thenReturn(rules);
        when(roomy.getGamerules()).thenReturn(rules);
        when(roomy.joinParty(members)).thenReturn(true);
        put(almostFull);
        put(roomy);

        JoinNpcExecutor.perform(player, JoinNpcAction.parse("GROUP", "2x2"));

        verify(roomy).joinParty(members);
        verify(almostFull, never()).joinParty(anyList());
    }

    @Test
    void partyMember_cannotJoin() {
        Party party = mock(Party.class);
        when(party.isAdmin(player.getUniqueId())).thenReturn(false);
        when(party.getAdmin()).thenReturn(UUID.randomUUID());
        when(pm.getParty(player)).thenReturn(party);
        IArena a = arena("solo_1", GameStatus.waiting, 4, 1, 0);
        put(a);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
            JoinNpcExecutor.perform(player, JoinNpcAction.parse("GROUP", "solo_*"));
            JoinNpcExecutor.perform(player, JoinNpcAction.parse("ARENA", "solo_1"));
        }

        verify(messages, times(2)).send(eq(player), eq("PartyOnlyAdminCanJoin"), any());
        verify(a, never()).join(any());
    }

    @Test
    void arena_joinsByNameIgnoringCase() {
        IArena a = arena("Castle", GameStatus.waiting, 2, 1, 0);
        when(a.join(player)).thenReturn(true);
        put(a);
        JoinNpcExecutor.perform(player, JoinNpcAction.parse("ARENA", "castle"));
        verify(a).join(player);
    }

    @Test
    void arena_usesSameChecksAsJoinCommand() {
        put(arena("castle", GameStatus.playing, 2, 1, 2));
        JoinNpcExecutor.perform(player, JoinNpcAction.parse("ARENA", "castle"));
        verify(messages).send(player, "ArenaRunningError");

        JoinNpcExecutor.perform(player, JoinNpcAction.parse("ARENA", "ghost"));
        verify(messages).send(player, "ArenaNotExists");
    }

    @Test
    void network_isRefusedWithoutNetworkMode() {
        JoinNpcExecutor.perform(player, JoinNpcAction.parse("NETWORK", "solo"));
        verify(messages).send(player, "NpcNetworkUnavailable");
    }
}
