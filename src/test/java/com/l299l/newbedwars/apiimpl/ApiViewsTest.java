package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.ArenaState;
import com.l299l.newbedwars.api.ArenaView;
import com.l299l.newbedwars.api.TeamView;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiViewsTest {
    private IArena arena;
    private Team red;
    private final NewBedwarsApiImpl api = new NewBedwarsApiImpl();

    @BeforeEach
    void setUp() {
        arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("A1");
        red = new Team(ChatColor.RED, "Red", arena);
        HashMap<String, Team> teams = new HashMap<>();
        teams.put("Red", red);
        when(arena.getTeams()).thenReturn(teams);
        when(arena.getTeam("Red")).thenReturn(red);
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        Arena.arenaByName.put("A1", arena);
    }

    @AfterEach
    void tearDown() {
        Arena.arenaByName.clear();
        Arena.arenaByWorld.clear();
    }

    @Test
    void stateMapsEveryGameStatus() {
        for (GameStatus status : GameStatus.values()) {
            when(arena.status()).thenReturn(status);
            assertEquals(ArenaState.valueOf(status.name().toUpperCase()), new ApiArena(arena).getState());
        }
    }

    @Test
    void arenaExposesBasicData() {
        World world = mock(World.class);
        when(arena.getArenaWorld()).thenReturn(world);
        when(arena.isEnabled()).thenReturn(true);
        when(arena.getMinPlayers()).thenReturn(2);
        when(arena.getMaxInTeam()).thenReturn(4);
        when(arena.getGameTime()).thenReturn(90);
        when(arena.getCurrentGamePhase()).thenReturn("Diamond 2");
        ArenaView view = new ApiArena(arena);
        assertEquals("A1", view.getName());
        assertTrue(view.isEnabled());
        assertSame(world, view.getWorld());
        assertEquals(2, view.getMinPlayers());
        assertEquals(4, view.getMaxInTeam());
        assertEquals(90, view.getGameTime());
        assertEquals("Diamond 2", view.getCurrentPhase());
    }

    @Test
    void nullNumbersBecomeZero() {
        when(arena.getMinPlayers()).thenReturn(null);
        when(arena.getMaxInTeam()).thenReturn(null);
        when(arena.getGameTime()).thenReturn(null);
        when(arena.isEnabled()).thenReturn(null);
        ArenaView view = new ApiArena(arena);
        assertEquals(0, view.getMinPlayers());
        assertEquals(0, view.getMaxInTeam());
        assertEquals(0, view.getGameTime());
        assertFalse(view.isEnabled());
    }

    @Test
    void playersAreResolvedFromUuids() {
        UUID online = UUID.randomUUID();
        UUID offline = UUID.randomUUID();
        Player player = mock(Player.class);
        when(arena.getPlayers()).thenReturn(Set.of(online, offline));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(online)).thenReturn(player);
            bukkit.when(() -> Bukkit.getPlayer(offline)).thenReturn(null);
            assertEquals(List.of(player), new ApiArena(arena).getPlayers());
        }
    }

    @Test
    void teamViewReflectsTeamAndNullBedIsNotDestroyed() {
        Optional<TeamView> team = new ApiArena(arena).getTeam("Red");
        assertTrue(team.isPresent());
        assertEquals("Red", team.get().getName());
        assertEquals(ChatColor.RED, team.get().getColor());
        assertFalse(team.get().isBedDestroyed());
        assertTrue(team.get().getPlayers().isEmpty());
        assertTrue(new ApiArena(arena).getTeam("Nope").isEmpty());
        assertEquals(1, new ApiArena(arena).getTeams().size());
    }

    @Test
    void teamViewPlayersAreACopy() {
        Player player = mock(Player.class);
        red.addPlayer(player);
        TeamView view = new ApiTeam(red);
        view.getPlayers().clear();
        assertEquals(1, red.getPlayers().size());
    }

    @Test
    void gamePlayerViewReflectsStats() {
        UUID id = UUID.randomUUID();
        GamePlayer gp = mock(GamePlayer.class);
        Player player = mock(Player.class);
        when(gp.getPlayer()).thenReturn(player);
        when(gp.getTeam()).thenReturn(red);
        when(gp.getKills()).thenReturn(3);
        when(gp.getFinalKills()).thenReturn(1);
        when(gp.getDeaths()).thenReturn(2);
        when(gp.getBedsBroken()).thenReturn(1);
        when(arena.getPlayer(id)).thenReturn(gp);
        var view = new ApiArena(arena).getGamePlayer(id).orElseThrow();
        assertSame(player, view.getPlayer());
        assertEquals("Red", view.getTeam().orElseThrow().getName());
        assertEquals(3, view.getKills());
        assertEquals(1, view.getFinalKills());
        assertEquals(2, view.getDeaths());
        assertEquals(1, view.getBedsBroken());
        assertTrue(new ApiArena(arena).getGamePlayer(UUID.randomUUID()).isEmpty());
    }

    @Test
    void gamePlayerWithoutTeamHasEmptyTeam() {
        UUID id = UUID.randomUUID();
        GamePlayer gp = mock(GamePlayer.class);
        when(arena.getPlayer(id)).thenReturn(gp);
        assertTrue(new ApiArena(arena).getGamePlayer(id).orElseThrow().getTeam().isEmpty());
    }

    @Test
    void apiFindsArenasByNameWorldAndPlayer() {
        World world = mock(World.class);
        Player inArena = mock(Player.class);
        Player spectator = mock(Player.class);
        Player outsider = mock(Player.class);
        Arena.arenaByWorld.put(world, arena);
        when(arena.isPlayerInArena(inArena)).thenReturn(true);
        when(arena.isPlayerInArena(spectator)).thenReturn(false);
        when(arena.isPlayerInArena(outsider)).thenReturn(false);
        when(arena.getSpectators()).thenReturn(new ArrayList<>(List.of(spectator)));

        assertEquals("A1", api.getArena("A1").orElseThrow().getName());
        assertTrue(api.getArena("missing").isEmpty());
        assertEquals("A1", api.getArena(world).orElseThrow().getName());
        assertEquals("A1", api.getArena(inArena).orElseThrow().getName());
        assertEquals("A1", api.getArena(spectator).orElseThrow().getName());
        assertTrue(api.getArena(outsider).isEmpty());
        assertEquals(1, api.getArenas().size());
    }

    @Test
    void viewsOfTheSameArenaAreEqual() {
        assertEquals(new ApiArena(arena), new ApiArena(arena));
        assertEquals(new ApiTeam(red), new ApiTeam(red));
    }

    @Test
    void getThrowsWhenPluginIsNotEnabled() {
        com.l299l.newbedwars.api.ApiHolder.set(null);
        assertThrows(IllegalStateException.class, com.l299l.newbedwars.api.NewBedwarsAPI::get);
        com.l299l.newbedwars.api.ApiHolder.set(api);
        assertSame(api, com.l299l.newbedwars.api.NewBedwarsAPI.get());
        com.l299l.newbedwars.api.ApiHolder.set(null);
    }

    @Test
    void generatorsAreExposedByKind() {
        var diamond = mock(com.l299l.newbedwars.arena.generators.Generator.class);
        when(diamond.getType()).thenReturn(com.l299l.newbedwars.arena.generators.GeneratorType.DIAMOND);
        var location = mock(org.bukkit.Location.class);
        when(location.clone()).thenReturn(location);
        when(diamond.getLocation()).thenReturn(location);
        when(arena.getGenerators()).thenReturn(List.of(diamond));
        var generators = new ApiArena(arena).getGenerators();
        assertEquals(1, generators.size());
        assertEquals(com.l299l.newbedwars.api.GeneratorKind.DIAMOND, generators.get(0).getKind());
        assertSame(location, generators.get(0).getLocation());
    }

    @Test
    void unknownUpgradeAndMissingUpgradesGiveLevelZero() {
        assertEquals(0, new ApiTeam(red).getUpgradeLevel("SHARPNESS"));
        assertEquals(0, new ApiTeam(red).getUpgradeLevel("NOT_AN_UPGRADE"));
    }

    @Test
    void extraDataOfArenaAndTeamIsSeparatePerAddon() {
        var addon = mock(org.bukkit.plugin.Plugin.class);
        when(addon.getName()).thenReturn("MyAddon");
        var json = new com.google.gson.JsonObject();
        when(arena.getExtra()).thenReturn(json);
        try (MockedStatic<Bukkit> ignored = mockStatic(Bukkit.class)) {
            // saving is only requested when a value changes; reading must not need Bukkit
            assertEquals(5, new ApiArena(arena).getExtra(addon).getInt("x", 5));
        }
        assertEquals("d", new ApiTeam(red).getExtra(addon).getString("x", "d"));
    }
}
