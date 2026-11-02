package com.l299l.newbedwars.arena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.group.MapVoteManager;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.player.PlayerStateResetter;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.IronGolemLogic;
import com.l299l.newbedwars.arena.shops.customitems.customitemlogic.logics.SilverfishLogic;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.data.DataManager;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUIManager;
import com.l299l.newbedwars.player.LobbyService;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArenaTest {

    private NewBedwars plugin;
    private PlayerManager playerManager;
    private DataManager dataManager;
    private GUIManager guiManager;
    private BukkitScheduler scheduler;
    private World world;
    private Arena arena;

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<ApiEvents> apiEvents;
    private MockedStatic<MapVoteManager> voteManager;
    private MockedStatic<LobbyService> lobby;
    private MockedStatic<PlayerStateResetter> resetter;
    private MockedStatic<IronGolemLogic> golems;
    private MockedStatic<SilverfishLogic> silverfish;

    private Integer oldMin, oldMaxInTeam, oldWaiting, oldRespawn, oldRejoinTime;
    private Boolean oldTablist, oldRejoinEnabled;

    @BeforeEach
    void setUp() {
        oldMin = Properties.DefaultMinPlayers;
        oldMaxInTeam = Properties.DefaultMaxInTeam;
        oldWaiting = Properties.DefaultWaitingTime;
        oldRespawn = Properties.DefaultRespawnTime;
        oldRejoinTime = Properties.RejoinTime;
        oldTablist = Properties.TablistIsolationEnabled;
        oldRejoinEnabled = Properties.RejoinTimeEnabled;
        Properties.DefaultMinPlayers = 2;
        Properties.DefaultMaxInTeam = 1;
        Properties.DefaultWaitingTime = 10;
        Properties.DefaultRespawnTime = 5;
        Properties.RejoinTime = 1;
        Properties.TablistIsolationEnabled = false;
        Properties.RejoinTimeEnabled = false;

        plugin = mock(NewBedwars.class);
        playerManager = mock(PlayerManager.class);
        dataManager = mock(DataManager.class);
        guiManager = mock(GUIManager.class);
        Messages messages = mock(Messages.class);
        when(plugin.getName()).thenReturn("NewBedwars");
        when(plugin.getGeneratorLeveling()).thenReturn(new HashMap<>());
        when(plugin.getScoreboardManager()).thenReturn(mock(ScoreboardManager.class));
        when(plugin.getPlayerManager()).thenReturn(playerManager);
        when(plugin.getDataManager()).thenReturn(dataManager);
        when(plugin.getGuiManager()).thenReturn(guiManager);
        when(plugin.getMessages()).thenReturn(messages);
        NewBedwars.plugin = plugin;

        scheduler = mock(BukkitScheduler.class);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getServer).thenReturn(mock(org.bukkit.Server.class));
        apiEvents = mockStatic(ApiEvents.class);
        apiEvents.when(() -> ApiEvents.playerJoin(any(), any(), anyBoolean())).thenReturn(true);
        voteManager = mockStatic(MapVoteManager.class);
        lobby = mockStatic(LobbyService.class);
        resetter = mockStatic(PlayerStateResetter.class);
        golems = mockStatic(IronGolemLogic.class);
        silverfish = mockStatic(SilverfishLogic.class);

        world = mock(World.class);
        when(world.getName()).thenReturn("castle_world");
        arena = spy(new Arena("Castle", world));
        ArenaRegistry.register("Castle", arena);
        ArenaRegistry.bindWorld(world, arena);
    }

    @AfterEach
    void tearDown() {
        silverfish.close();
        golems.close();
        resetter.close();
        lobby.close();
        voteManager.close();
        apiEvents.close();
        bukkit.close();
        ArenaRegistry.clear();
        NewBedwars.plugin = null;
        Properties.DefaultMinPlayers = oldMin;
        Properties.DefaultMaxInTeam = oldMaxInTeam;
        Properties.DefaultWaitingTime = oldWaiting;
        Properties.DefaultRespawnTime = oldRespawn;
        Properties.RejoinTime = oldRejoinTime;
        Properties.TablistIsolationEnabled = oldTablist;
        Properties.RejoinTimeEnabled = oldRejoinEnabled;
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> map(String field) throws Exception {
        Field f = Arena.class.getDeclaredField(field);
        f.setAccessible(true);
        return (Map<K, V>) f.get(arena);
    }

    private Player player(String name, World in) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        when(player.getWorld()).thenReturn(in);
        when(player.isOnline()).thenReturn(true);
        return player;
    }

    private GamePlayer gamePlayer(Player player, Team team) {
        GamePlayer gp = mock(GamePlayer.class);
        when(gp.getPlayer()).thenReturn(player);
        when(gp.getTeam()).thenReturn(team);
        return gp;
    }

    private Team team(String name, boolean alive, Player... members) {
        Team team = mock(Team.class);
        when(team.getName()).thenReturn(name);
        when(team.isAlive()).thenReturn(alive);
        when(team.getPlayers()).thenReturn(new ArrayList<>(List.of(members)));
        when(team.colored(anyString())).thenAnswer(inv -> inv.getArgument(0));
        return team;
    }

    private GamePlayer join(Player player, Team team) throws Exception {
        GamePlayer gp = gamePlayer(player, team);
        map("players").put(player.getUniqueId(), gp);
        return gp;
    }

    @Test
    void persistStatsIncludesPlayersRemovedDuringTheGame() throws Exception {
        Player alice = player("Alice", world);
        Player bob = player("Bob", world);
        Team winner = team("Red", true, alice);
        Team loser = team("Blue", false);
        GamePlayer aliceGp = join(alice, winner);
        GamePlayer bobGp = join(bob, loser);

        arena.retire(bob.getUniqueId());
        arena.persistStats(winner);

        assertFalse(arena.isPlayerInArena(bob));
        verify(playerManager).addGameResult("Alice", aliceGp, true);
        verify(playerManager).addGameResult("Bob", bobGp, false);
        verify(dataManager, times(2)).recordGameResult(anyString(), any());
        verify(dataManager).savePlayerStats();
    }

    @Test
    void persistStatsCountsAPlayerOnlyOnce() throws Exception {
        Player alice = player("Alice", world);
        Team team = team("Red", true);
        GamePlayer gp = join(alice, team);

        arena.retire(alice.getUniqueId());
        map("players").put(alice.getUniqueId(), gp);
        arena.retire(alice.getUniqueId());
        map("players").put(alice.getUniqueId(), gp);
        arena.persistStats(null);

        verify(playerManager, times(1)).addGameResult("Alice", gp, false);
    }

    @Test
    void lobbyPlayersWithoutATeamAreNotRecorded() throws Exception {
        Player alice = player("Alice", world);
        join(alice, null);

        arena.retire(alice.getUniqueId());
        arena.persistStats(null);

        verify(playerManager, never()).addGameResult(anyString(), any(), anyBoolean());
    }

    @Test
    void leavingARunningGameKeepsTheStats() throws Exception {
        arena.setStatus(GameStatus.playing);
        Player alice = player("Alice", world);
        Team red = team("Red", true, alice);
        Team blue = team("Blue", true);
        map("teams").put("Red", red);
        map("teams").put("Blue", blue);
        GamePlayer gp = join(alice, red);

        arena.leave(alice, LeaveReason.DISCONNECT);

        assertFalse(arena.isPlayerInArena(alice));
        verify(red).removePlayer(alice);
        arena.persistStats(blue);
        verify(playerManager).addGameResult("Alice", gp, false);
    }

    @Test
    void rejoinTimeoutKeepsTheStats() throws Exception {
        Properties.RejoinTimeEnabled = true;
        arena.setStatus(GameStatus.playing);
        Player alice = player("Alice", world);
        Team red = team("Red", true, alice);
        Team blue = team("Blue", true);
        map("teams").put("Red", red);
        map("teams").put("Blue", blue);
        GamePlayer gp = join(alice, red);

        arena.leave(alice, LeaveReason.DISCONNECT);

        assertTrue(arena.isPlayerInArena(alice));
        ArgumentCaptor<Runnable> expire = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), expire.capture(), eq(1200L));
        expire.getValue().run();

        assertFalse(arena.isPlayerInArena(alice));
        arena.persistStats(blue);
        verify(playerManager).addGameResult("Alice", gp, false);
    }

    @Test
    void leavingThatLeavesOneTeamEndsTheGame() throws Exception {
        arena.setStatus(GameStatus.playing);
        doNothing().when(arena).startEnding(any());
        Player alice = player("Alice", world);
        Team red = team("Red", false, alice);
        Team blue = team("Blue", true);
        map("teams").put("Red", red);
        map("teams").put("Blue", blue);
        join(alice, red);

        arena.leave(alice, LeaveReason.DISCONNECT);

        verify(arena).startEnding(blue);
        assertFalse(arena.isPlayerInArena(alice));
    }

    @Test
    void leavingTheWaitingLobbyRemovesThePlayer() throws Exception {
        arena.setStatus(GameStatus.waiting);
        Player alice = player("Alice", world);
        join(alice, null);

        arena.leave(alice, LeaveReason.DISCONNECT);

        assertFalse(arena.isPlayerInArena(alice));
        apiEvents.verify(() -> ApiEvents.playerLeave(any(), eq(alice), eq(LeaveReason.DISCONNECT)));
    }

    @Test
    void drawEndsWithoutAWinner() {
        doNothing().when(arena).startEnding(any());

        arena.endGame("draw");

        verify(arena).startEnding(null);
    }

    @Test
    void mostKillsPicksTheTeamWithTheMostKills() throws Exception {
        doNothing().when(arena).startEnding(any());
        Player a = player("A", world);
        Player b = player("B", world);
        Player c = player("C", world);
        Team red = team("Red", true, a);
        Team blue = team("Blue", true, b, c);
        map("teams").put("Red", red);
        map("teams").put("Blue", blue);
        GamePlayer gpA = join(a, red);
        GamePlayer gpB = join(b, blue);
        GamePlayer gpC = join(c, blue);
        when(gpA.getKills()).thenReturn(2);
        when(gpB.getKills()).thenReturn(1);
        when(gpC.getKills()).thenReturn(3);

        arena.endGame("MOST_KILLS");

        verify(arena).startEnding(blue);
    }

    @Test
    void unknownCriteriaJustStopsTheArena() {
        doNothing().when(arena).stop();

        arena.endGame("whatever");

        verify(arena).stop();
        verify(arena, never()).startEnding(any());
    }

    @Test
    void shutdownOnlyLeavesPlayersStillInTheArena() throws Exception {
        doNothing().when(arena).leave(any(), any());
        World lobbyWorld = mock(World.class);
        Player inside = player("Inside", world);
        Player away = player("Away", lobbyWorld);
        Player offline = player("Offline", world);
        join(inside, null);
        join(away, null);
        join(offline, null);
        bukkit.when(() -> Bukkit.getPlayer(inside.getUniqueId())).thenReturn(inside);
        bukkit.when(() -> Bukkit.getPlayer(away.getUniqueId())).thenReturn(away);
        bukkit.when(() -> Bukkit.getPlayer(offline.getUniqueId())).thenReturn(null);

        arena.forceShutdown();

        verify(arena).leave(inside, LeaveReason.KICKED);
        verify(arena, never()).leave(eq(away), any());
        verify(arena, never()).leave(eq(offline), any());
        assertTrue(arena.getPlayers().isEmpty());
    }

    @Test
    void watchEndingGivesTheLeaveItemToARespawnedSpectator() throws Exception {
        arena.setStatus(GameStatus.ending);
        Player watcher = player("Watcher", world);
        Player gone = player("Gone", world);
        Player away = player("Away", mock(World.class));
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(watcher.getInventory()).thenReturn(inventory);
        ItemStack leaveItem = mock(ItemStack.class);
        when(guiManager.getLeaveItem(watcher)).thenReturn(leaveItem);
        join(watcher, null);
        join(gone, null);
        join(away, null);
        arena.getSpectators().add(watcher);
        bukkit.when(() -> Bukkit.getPlayer(watcher.getUniqueId())).thenReturn(watcher);
        bukkit.when(() -> Bukkit.getPlayer(gone.getUniqueId())).thenReturn(null);
        bukkit.when(() -> Bukkit.getPlayer(away.getUniqueId())).thenReturn(away);

        arena.watchEnding();

        verify(inventory).setItem(8, leaveItem);
        assertFalse(arena.isPlayerInArena(gone));
        assertTrue(arena.isPlayerInArena(away));
        assertFalse(arena.getSpectators().contains(away));
    }

    @Test
    void joinAsSpectatorIsRefusedWhilePlayingInAnotherArena() {
        arena.setStatus(GameStatus.playing);
        arena.getGamerules().AllowSpectators = true;
        World otherWorld = mock(World.class);
        Player viewer = player("Viewer", otherWorld);
        IArena other = mock(IArena.class);
        when(other.isPlayerInArena(viewer)).thenReturn(true);
        when(other.getArenaWorld()).thenReturn(otherWorld);
        ArenaRegistry.register("Other", other);

        assertFalse(arena.joinAsSpectator(viewer));

        verify(other, never()).forfeitRejoin(any());
    }

    @Test
    void joinAsSpectatorForfeitsRejoinSlotsInOtherArenas() {
        arena.setStatus(GameStatus.playing);
        arena.getGamerules().AllowSpectators = true;
        Player viewer = player("Viewer", world);
        when(viewer.getInventory()).thenReturn(mock(PlayerInventory.class));
        IArena other = mock(IArena.class);
        ArenaRegistry.register("Other", other);

        assertTrue(arena.joinAsSpectator(viewer));

        verify(other).forfeitRejoin(viewer);
    }
}
