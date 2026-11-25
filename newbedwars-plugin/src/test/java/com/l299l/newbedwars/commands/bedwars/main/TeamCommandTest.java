package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.arena.team.TeamSelectorManager;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamCommandTest {
    private TeamSelectorManager previous;
    private TeamSelectorManager manager;
    private Messages messages;
    private Player player;
    private World world;
    private IArena arena;
    private TeamCommand cmd;

    @BeforeEach
    void setUp() {
        previous = TeamSelectorManager.get();
        NewBedwars plugin = mock(NewBedwars.class);
        messages = mock(Messages.class);
        when(plugin.getMessages()).thenReturn(messages);
        NewBedwars.plugin = plugin;
        manager = mock(TeamSelectorManager.class);
        setInstance(manager);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        Team red = mock(Team.class);
        when(red.getName()).thenReturn("red");
        when(red.getDisplayName()).thenReturn("§cCrimson");
        Team blue = mock(Team.class);
        when(blue.getName()).thenReturn("blue");
        when(blue.getDisplayName()).thenReturn("Blue");
        Map<String, Team> teams = new LinkedHashMap<>();
        teams.put("red", red);
        teams.put("blue", blue);
        arena = mock(IArena.class);
        when(arena.getTeams()).thenReturn(teams);
        cmd = new TeamCommand();
    }

    @AfterEach
    void tearDown() {
        setInstance(previous);
        NewBedwars.plugin = null;
    }

    private static void setInstance(TeamSelectorManager value) {
        try {
            Field f = TeamSelectorManager.class.getDeclaredField("instance");
            f.setAccessible(true);
            f.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void noArenaOrNoManagerSendsNotNow() {
        cmd.perform(player, new String[]{"team"}, null);
        verify(messages).send(player, "TeamSelectorNotNow");
        setInstance(null);
        cmd.perform(player, new String[]{"team", "red"}, arena);
        verify(messages, times(2)).send(player, "TeamSelectorNotNow");
    }

    @Test
    void noArgumentOpensTheMenu() {
        cmd.perform(player, new String[]{"team"}, arena);
        verify(manager).open(player, arena);
    }

    @Test
    void randomClearsThePick() {
        cmd.perform(player, new String[]{"team", "RANDOM"}, arena);
        verify(manager).pick(player, arena, null);
    }

    @Test
    void teamIsFoundByNameOrStrippedDisplayNameIgnoringCase() {
        cmd.perform(player, new String[]{"team", "RED"}, arena);
        cmd.perform(player, new String[]{"team", "crimson"}, arena);
        verify(manager, times(2)).pick(player, arena, "red");
    }

    @Test
    void unknownTeamSendsUnknownTeamMessage() {
        cmd.perform(player, new String[]{"team", "green"}, arena);
        verify(messages).send(player, "TeamSelectorUnknownTeam", Map.of("/team/", "green"));
        verify(manager, never()).pick(any(), any(), anyString());
    }

    @Test
    void disabledArenaGivesDisabledMessageThroughRealManager() {
        TeamSelectorManager real = new TeamSelectorManager();
        Gamerules rules = mock(Gamerules.class);
        rules.TeamSelection = false;
        when(arena.getGamerules()).thenReturn(rules);
        cmd.perform(player, new String[]{"team"}, arena);
        verify(messages).send(player, "TeamSelectorDisabled");
        assertFalse(real.canPickNow(player, arena));
    }

    @Test
    void notWaitingGivesNotNowMessageThroughRealManager() {
        new TeamSelectorManager();
        Gamerules rules = mock(Gamerules.class);
        rules.TeamSelection = true;
        when(arena.getGamerules()).thenReturn(rules);
        when(player.hasPermission(TeamSelectorManager.PERMISSION)).thenReturn(true);
        when(arena.isPlayerInArena(player)).thenReturn(true);
        when(arena.status()).thenReturn(GameStatus.playing);
        cmd.perform(player, new String[]{"team", "red"}, arena);
        verify(messages).send(player, "TeamSelectorNotNow");
    }

    @Test
    void tabCompletionListsTeamsAndRandomWhenPickingIsPossible() {
        when(manager.canPickNow(player, arena)).thenReturn(true);
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            assertTrue(cmd.isAvailable(player));
            assertEquals(List.of("red", "blue", "random"), cmd.getSubcommandArguments(player, new String[]{"team", ""}));
            assertNull(cmd.getSubcommandArguments(player, new String[]{"team", "red", ""}));
            assertNull(cmd.getSubcommandArguments(player, new String[]{"team"}));
        }
    }

    @Test
    void tabCompletionHiddenWhenPickingIsNotPossible() {
        when(manager.canPickNow(player, arena)).thenReturn(false);
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            assertFalse(cmd.isAvailable(player));
            assertNull(cmd.getSubcommandArguments(player, new String[]{"team", ""}));
        }
        setInstance(null);
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            assertFalse(cmd.isAvailable(player));
        }
    }

    @Test
    void metadata() {
        assertEquals("team", cmd.getName());
        assertEquals("/bw team [team|random]", cmd.getSyntax());
    }
}
