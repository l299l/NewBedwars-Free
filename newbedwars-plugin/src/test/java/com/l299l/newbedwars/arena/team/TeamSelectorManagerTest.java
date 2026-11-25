package com.l299l.newbedwars.arena.team;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamSelectorManagerTest {
    private TeamSelectorManager previousInstance;
    private TeamSelectorManager manager;
    private Messages messages;
    private IArena arena;
    private Gamerules rules;
    private Team red;
    private Team blue;
    private TeamPicks picks;
    private Player p1;
    private Player p2;
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<ApiEvents> events;

    @BeforeEach
    void setUp() {
        previousInstance = TeamSelectorManager.get();
        NewBedwars plugin = mock(NewBedwars.class);
        messages = mock(Messages.class);
        when(plugin.getMessages()).thenReturn(messages);
        when(messages.getMsg(any(Player.class), anyString())).thenAnswer(i -> i.getArgument(1));
        NewBedwars.plugin = plugin;
        manager = new TeamSelectorManager();

        p1 = player("One");
        p2 = player("Two");
        picks = new TeamPicks();
        rules = mock(Gamerules.class);
        rules.TeamSelection = true;
        rules.TeamSelectionBalanced = true;
        red = team("red", ChatColor.RED);
        blue = team("blue", ChatColor.BLUE);
        arena = mock(IArena.class);
        Map<String, Team> teams = new LinkedHashMap<>();
        teams.put("red", red);
        teams.put("blue", blue);
        when(arena.getTeams()).thenReturn(teams);
        when(arena.getGamerules()).thenReturn(rules);
        when(arena.getTeamPicks()).thenReturn(picks);
        when(arena.getMaxInTeam()).thenReturn(2);
        when(arena.status()).thenReturn(GameStatus.waiting);
        for (Player p : List.of(p1, p2)) {
            when(arena.isPlayerInArena(p)).thenReturn(true);
            when(arena.pickGroup(p)).thenReturn(List.of(p));
        }
        when(red.getArena()).thenReturn(arena);
        when(blue.getArena()).thenReturn(arena);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getPlayer(p1.getUniqueId())).thenReturn(p1);
        bukkit.when(() -> Bukkit.getPlayer(p2.getUniqueId())).thenReturn(p2);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(new ArrayList<Player>());
        bukkit.when(Bukkit::getItemFactory).thenReturn(mock(ItemFactory.class));
        events = mockStatic(ApiEvents.class);
        events.when(() -> ApiEvents.teamPick(any(), any(), any(), any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        events.close();
        bukkit.close();
        Field f = TeamSelectorManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, previousInstance);
        NewBedwars.plugin = null;
    }

    private Player player(String name) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getName()).thenReturn(name);
        when(p.hasPermission(TeamSelectorManager.PERMISSION)).thenReturn(true);
        when(p.getInventory()).thenReturn(mock(PlayerInventory.class));
        return p;
    }

    private static Team team(String name, ChatColor color) {
        Team t = mock(Team.class);
        when(t.getName()).thenReturn(name);
        when(t.getDisplayName()).thenReturn(name);
        when(t.getColor()).thenReturn(color);
        when(t.colored(anyString())).thenAnswer(i -> i.getArgument(0));
        return t;
    }

    @Test
    void validPickIsStoredAndAnnounced() {
        assertTrue(manager.pick(p1, arena, "red"));
        assertEquals("red", picks.get(p1.getUniqueId()));
        verify(p1).sendMessage("TeamSelectorPicked");
        verify(arena).onTeamPicksChanged();
        events.verify(() -> ApiEvents.teamPick(arena, p1, red, null, List.of(p1)));
    }

    @Test
    void repickingTheSameTeamFiresNoEvent() {
        manager.pick(p1, arena, "red");
        assertTrue(manager.pick(p1, arena, "red"));
        events.verify(() -> ApiEvents.teamPick(any(), any(), any(), any(), any()));
    }

    @Test
    void switchingTeamReportsThePreviousOne() {
        manager.pick(p1, arena, "red");
        assertTrue(manager.pick(p1, arena, "blue"));
        assertEquals("blue", picks.get(p1.getUniqueId()));
        events.verify(() -> ApiEvents.teamPick(arena, p1, blue, red, List.of(p1)));
    }

    @Test
    void unbalancedPickIsRefused() {
        manager.pick(p2, arena, "red");
        assertFalse(manager.pick(p1, arena, "red"));
        verify(messages).send(p1, "TeamSelectorTeamUnbalanced");
        assertNull(picks.get(p1.getUniqueId()));
        assertEquals(TeamPicks.Result.UNBALANCED, manager.check(p1, arena, "red"));
        assertEquals(TeamPicks.Result.OK, manager.check(p1, arena, "blue"));
    }

    @Test
    void unbalancedPickAllowedWhenBalancingIsOff() {
        rules.TeamSelectionBalanced = false;
        manager.pick(p2, arena, "red");
        assertTrue(manager.pick(p1, arena, "red"));
        assertEquals(Map.of("red", 2), manager.pickedCounts(arena));
    }

    @Test
    void fullTeamIsRefused() {
        rules.TeamSelectionBalanced = false;
        when(arena.getMaxInTeam()).thenReturn(1);
        manager.pick(p2, arena, "red");
        assertFalse(manager.pick(p1, arena, "red"));
        verify(messages).send(p1, "TeamSelectorTeamFull");
        assertEquals(TeamPicks.Result.FULL, manager.check(p1, arena, "red"));
    }

    @Test
    void groupLargerThanTheTeamIsRefusedWithPartyMessage() {
        when(arena.getMaxInTeam()).thenReturn(1);
        when(arena.pickGroup(p1)).thenReturn(List.of(p1, p2));
        assertFalse(manager.pick(p1, arena, "red"));
        verify(messages).send(p1, "TeamSelectorPartyTooBig");
    }

    @Test
    void unknownTeamIsRefused() {
        assertFalse(manager.pick(p1, arena, "green"));
        verify(p1).sendMessage("TeamSelectorUnknownTeam");
        assertTrue(picks.isEmpty());
    }

    @Test
    void emptyGroupMeansOnlyTheLeaderMayPick() {
        when(arena.pickGroup(p1)).thenReturn(List.of());
        assertFalse(manager.pick(p1, arena, "red"));
        verify(messages).send(p1, "TeamSelectorLeaderOnly");
    }

    @Test
    void randomClearsAnExistingPick() {
        manager.pick(p1, arena, "red");
        assertTrue(manager.pick(p1, arena, null));
        assertNull(picks.get(p1.getUniqueId()));
        verify(messages).send(p1, "TeamSelectorCleared");
        events.verify(() -> ApiEvents.teamPick(arena, p1, null, red, List.of(p1)));
    }

    @Test
    void randomWithoutPickFiresNoEventButStillSucceeds() {
        assertTrue(manager.pick(p1, arena, null));
        events.verify(() -> ApiEvents.teamPick(any(), any(), any(), any(), any()), never());
    }

    @Test
    void cancelledEventKeepsThePick() {
        events.when(() -> ApiEvents.teamPick(any(), any(), any(), any(), any())).thenReturn(false);
        assertFalse(manager.pick(p1, arena, "red"));
        assertTrue(picks.isEmpty());
        verify(arena, never()).onTeamPicksChanged();
    }

    @Test
    void refusedWhenDisabledNoPermissionOrNotWaiting() {
        rules.TeamSelection = false;
        assertFalse(manager.pick(p1, arena, "red"));
        verify(messages).send(p1, "TeamSelectorDisabled");

        rules.TeamSelection = true;
        when(p2.hasPermission(TeamSelectorManager.PERMISSION)).thenReturn(false);
        assertFalse(manager.pick(p2, arena, "red"));
        verify(messages).send(p2, "TeamSelectorNoPermission");

        when(arena.status()).thenReturn(GameStatus.playing);
        assertFalse(manager.pick(p1, arena, "red"));
        verify(messages).send(p1, "TeamSelectorNotNow");
        assertTrue(picks.isEmpty());
    }

    @Test
    void canPickNowFollowsStateAndPermission() {
        assertTrue(manager.canPickNow(p1, arena));
        when(arena.status()).thenReturn(GameStatus.starting);
        assertTrue(manager.canPickNow(p1, arena));
        when(arena.status()).thenReturn(GameStatus.playing);
        assertFalse(manager.canPickNow(p1, arena));
        when(arena.status()).thenReturn(GameStatus.waiting);
        when(arena.isPlayerInArena(p1)).thenReturn(false);
        assertFalse(manager.canPickNow(p1, arena));
        assertFalse(manager.canPickNow(p2, null));
        when(p2.hasPermission(TeamSelectorManager.PERMISSION)).thenReturn(false);
        assertFalse(manager.canPickNow(p2, arena));
    }

    @Test
    void pickedTeamCountsAndPlayersIncludeTheWholeGroup() {
        when(arena.pickGroup(p1)).thenReturn(List.of(p1, p2));
        manager.pick(p1, arena, "blue");
        assertEquals("blue", manager.pickedTeam(p1, arena));
        assertEquals("blue", manager.pickedTeam(p2, arena));
        assertEquals(Map.of("blue", 2), manager.pickedCounts(arena));
        assertEquals(List.of(p1, p2), manager.pickedPlayers(arena, "blue"));
        assertTrue(manager.pickedPlayers(arena, "red").isEmpty());
        when(arena.isPlayerInArena(p2)).thenReturn(false);
        assertNull(manager.pickedTeam(p2, arena));
        assertNull(manager.pickedTeam(p1, null));
    }

    @Test
    void enabledNeedsArenaAndGamerules() {
        assertTrue(manager.enabled(arena));
        assertFalse(manager.enabled(null));
        when(arena.getGamerules()).thenReturn(null);
        assertFalse(manager.enabled(arena));
        when(arena.getGamerules()).thenReturn(rules);
        rules.TeamSelection = null;
        assertFalse(manager.enabled(arena));
    }

    @Test
    void openRefusedWhenNotUsable() {
        when(arena.status()).thenReturn(GameStatus.playing);
        manager.open(p1, arena);
        verify(messages).send(p1, "TeamSelectorNotNow");
        verify(p1, never()).openInventory(any(org.bukkit.inventory.Inventory.class));
    }
}
