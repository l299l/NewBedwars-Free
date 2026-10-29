package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.apiimpl.edit.ApiEditor;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.edit.ArenaEditor;
import com.l299l.newbedwars.api.view.GeneratorKind;
import com.l299l.newbedwars.api.edit.TeamEditor;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ApiEditorTest {
    private MockedStatic<Bukkit> bukkit;
    private IArena arena;
    private World world;
    private Map<String, Team> teams;
    private List<Generator> generators;
    private ApiEditor editor;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));
        NewBedwars.plugin = mock(NewBedwars.class);
        world = mock(World.class);
        arena = mock(IArena.class);
        teams = new HashMap<>();
        generators = new ArrayList<>();
        when(arena.getArenaName()).thenReturn("A1");
        when(arena.status()).thenReturn(GameStatus.waiting);
        when(arena.getArenaWorld()).thenReturn(world);
        when(arena.getTeam(anyString())).thenAnswer(inv -> teams.get(inv.<String>getArgument(0)));
        doAnswer(inv -> {
            String name = inv.getArgument(0);
            teams.put(name, new Team(inv.getArgument(1), name, arena));
            return null;
        }).when(arena).createTeam(anyString(), any(ChatColor.class));
        when(arena.removeTeam(anyString())).thenAnswer(inv -> teams.remove(inv.<String>getArgument(0)) != null);
        doAnswer(inv -> generators.add(inv.getArgument(0))).when(arena).addGenerator(any());
        when(arena.removeGenerator(anyInt())).thenAnswer(inv -> {
            int i = inv.getArgument(0);
            return i >= 0 && i < generators.size() ? generators.remove(i) : null;
        });
        com.l299l.newbedwars.arena.Arena.arenaByWorld.put(world, arena);
        editor = new ApiEditor(arena);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
        com.l299l.newbedwars.arena.Arena.arenaByWorld.clear();
        bukkit.close();
    }

    private Location inArena() {
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.clone()).thenReturn(location);
        return location;
    }

    @Test
    void onlyAWaitingArenaCanBeEdited() {
        for (GameStatus status : new GameStatus[]{GameStatus.starting, GameStatus.playing, GameStatus.ending, GameStatus.restarting}) {
            when(arena.status()).thenReturn(status);
            assertThrows(IllegalStateException.class, () -> new ApiEditor(arena), status.name());
            assertThrows(IllegalStateException.class, () -> editor.setMinPlayers(2), status.name());
            assertThrows(IllegalStateException.class, () -> editor.createTeam("Red", ChatColor.RED), status.name());
        }
    }

    @Test
    void settingsAreValidatedAndApplied() {
        editor.setMinPlayers(2);
        verify(arena).setMinPlayers(2);
        editor.setMaxInTeam(4);
        verify(arena).setMaxInTeam(4);
        assertThrows(IllegalArgumentException.class, () -> editor.setMinPlayers(0));
        assertThrows(IllegalArgumentException.class, () -> editor.setMaxInTeam(0));
    }

    @Test
    void waitingSpawnMustBeInTheArenaWorld() {
        Location ok = inArena();
        editor.setWaitingSpawn(ok);
        verify(arena).setWaitingSpawn(ok);
        Location elsewhere = mock(Location.class);
        when(elsewhere.getWorld()).thenReturn(mock(World.class));
        assertThrows(IllegalArgumentException.class, () -> editor.setWaitingSpawn(elsewhere));
        assertThrows(IllegalArgumentException.class, () -> editor.setWaitingSpawn(null));
    }

    @Test
    void teamsCanBeCreatedFoundAndRemoved() {
        TeamEditor red = editor.createTeam("Red", ChatColor.RED);
        assertEquals("Red", red.getName());
        assertTrue(teams.containsKey("Red"));
        assertTrue(editor.getTeam("Red").isPresent());
        assertTrue(editor.getTeam("Blue").isEmpty());
        assertTrue(editor.removeTeam("Red"));
        assertFalse(editor.removeTeam("Red"));
    }

    @Test
    void teamCreationIsValidated() {
        editor.createTeam("Red", ChatColor.RED);
        assertThrows(IllegalArgumentException.class, () -> editor.createTeam("Red", ChatColor.BLUE));
        assertThrows(IllegalArgumentException.class, () -> editor.createTeam(" ", ChatColor.BLUE));
        assertThrows(IllegalArgumentException.class, () -> editor.createTeam(null, ChatColor.BLUE));
        assertThrows(IllegalArgumentException.class, () -> editor.createTeam("Bold", ChatColor.BOLD));
        assertThrows(IllegalArgumentException.class, () -> editor.createTeam("Null", null));
    }

    @Test
    void teamColorChangesThroughTheTeam() {
        editor.createTeam("Red", ChatColor.RED).setColor(ChatColor.GOLD);
        assertEquals(ChatColor.GOLD, teams.get("Red").getColor());
        TeamEditor team = editor.getTeam("Red").orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> team.setColor(ChatColor.ITALIC));
    }

    @Test
    void teamSpawnBedAndGeneratorAreSetAndCleared() {
        TeamEditor team = editor.createTeam("Red", ChatColor.RED);
        Location spawn = inArena();
        team.setSpawn(spawn);
        assertSame(spawn, teams.get("Red").getTeamSpawn());
        Location gen = inArena();
        try (var constructed = mockConstruction(com.l299l.newbedwars.arena.generators.BasicGenerator.class)) {
            team.setGenerator(gen);
            assertEquals(1, constructed.constructed().size());
        }
        assertNotNull(teams.get("Red").getGenerator());
        team.clearGenerator();
        assertNull(teams.get("Red").getGenerator());
    }

    @Test
    void bedNeedsARealBedAtTheLocation() {
        TeamEditor team = editor.createTeam("Red", ChatColor.RED);
        Location location = inArena();
        Block block = mock(Block.class);
        when(location.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.STONE);
        assertThrows(IllegalArgumentException.class, () -> team.setBed(location));
        when(block.getType()).thenReturn(Material.RED_BED);
        when(location.getWorld()).thenReturn(world);
        team.setBed(location);
        assertTrue(teams.get("Red").isBedSet());
        team.clearBed();
        assertFalse(teams.get("Red").isBedSet());
    }

    @Test
    void shopAndUpgradeNpcsNeedLivingEntities() {
        TeamEditor team = editor.createTeam("Red", ChatColor.RED);
        Location location = inArena();
        assertThrows(IllegalArgumentException.class, () -> team.setShop(location, EntityType.ARROW));
        assertThrows(IllegalArgumentException.class, () -> team.setUpgrades(location, EntityType.ARROW));
        assertThrows(IllegalArgumentException.class, () -> team.setShop(location, null));
        team.setShop(location, EntityType.VILLAGER);
        team.setUpgrades(location, EntityType.VILLAGER);
        assertTrue(teams.get("Red").isShopSet());
        assertTrue(teams.get("Red").isUpgradesSet());
        team.clearShop();
        team.clearUpgrades();
        assertFalse(teams.get("Red").isShopSet());
        assertFalse(teams.get("Red").isUpgradesSet());
    }

    @Test
    void locationsOutsideTheArenaWorldAreRejected() {
        TeamEditor team = editor.createTeam("Red", ChatColor.RED);
        Location elsewhere = mock(Location.class);
        when(elsewhere.getWorld()).thenReturn(mock(World.class));
        assertThrows(IllegalArgumentException.class, () -> team.setSpawn(elsewhere));
        assertThrows(IllegalArgumentException.class, () -> team.setGenerator(elsewhere));
    }

    @Test
    void mapGeneratorsOnlyDiamondAndEmerald() {
        try (var diamond = mockConstruction(com.l299l.newbedwars.arena.generators.DiamondGenerator.class,
                     (m, c) -> when(m.getType()).thenReturn(GeneratorType.DIAMOND));
             var emerald = mockConstruction(com.l299l.newbedwars.arena.generators.EmeraldGenerator.class,
                     (m, c) -> when(m.getType()).thenReturn(GeneratorType.EMERALD))) {
            editor.addGenerator(GeneratorKind.DIAMOND, inArena());
            editor.addGenerator(GeneratorKind.EMERALD, inArena());
        }
        assertEquals(2, generators.size());
        assertEquals(GeneratorType.DIAMOND, generators.get(0).getType());
        assertEquals(GeneratorType.EMERALD, generators.get(1).getType());
        assertThrows(IllegalArgumentException.class, () -> editor.addGenerator(GeneratorKind.BASIC, inArena()));
        assertTrue(editor.removeGenerator(0));
        assertFalse(editor.removeGenerator(5));
        assertEquals(1, generators.size());
    }
}
