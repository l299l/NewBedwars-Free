package com.l299l.newbedwars.tablist;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TablistManagerTest {
    private static final String YAML = """
            Lobby:
              Enabled: true
              Header: ["lobby head"]
              PlayerFormat: "&7{player}"
            Layouts:
              Default:
                Waiting:
                  Header: ["W {arena} {players}/{maxPlayers}"]
                  Footer: ["wf"]
                  PlayerFormat: "{teamColor}{player}"
                Playing:
                  Header: ["P {phase}"]
                  PlayerFormat: "{teamColor}[{teamShort}] {player}"
                Spectator:
                  PlayerFormat: "&7{player}"
              Other:
                Playing:
                  Header: ["other"]
            """;

    private boolean savedEnabled;
    private String savedDefault;
    private final List<String> warnings = new ArrayList<>();
    private TablistManager manager;
    private Player player;
    private World world;
    private IArena arena;
    private Team team;

    @BeforeEach
    void setUp() {
        savedEnabled = Properties.TablistEnabled;
        savedDefault = Properties.DefaultTablist;
        Properties.TablistEnabled = true;
        Properties.DefaultTablist = "Default";
        NewBedwars.plugin = mock(NewBedwars.class);
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        manager = new TablistManager(TablistConfig.parse(YamlConfiguration.loadConfiguration(new java.io.StringReader(YAML)), log), log);
        player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn("Steve");
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("castle");
        when(arena.getActivePlayerCount()).thenReturn(3);
        when(arena.getMaxPlayers()).thenReturn(8);
        when(arena.getNextGamePhase()).thenReturn("Diamond II");
        when(arena.getPlayers()).thenReturn(Set.of(id));
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        when(arena.status()).thenReturn(GameStatus.waiting);
        team = mock(Team.class);
        when(team.getColor()).thenReturn(ChatColor.RED);
        when(team.getDisplayName()).thenReturn("Red");
        when(arena.getTeam(player)).thenReturn(team);
        when(arena.getPlayer(id)).thenReturn(mock(GamePlayer.class));
    }

    @AfterEach
    void tearDown() {
        Properties.TablistEnabled = savedEnabled;
        Properties.DefaultTablist = savedDefault;
        NewBedwars.plugin = null;
    }

    @Test
    void waitingPlayerGetsWaitingLayout() {
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            manager.update(player);
        }
        verify(player).setPlayerListHeaderFooter("W castle 3/8", "wf");
        verify(player).setPlayerListName(ChatColor.RED + "Steve");
    }

    @Test
    void playingPlayerGetsPlayingFormatAndSecondUpdateIsSkipped() {
        when(arena.status()).thenReturn(GameStatus.playing);
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            manager.update(player);
            manager.update(player);
        }
        verify(player).setPlayerListName(ChatColor.RED + "[R] Steve");
        verify(player).setPlayerListHeaderFooter("P Diamond II", "wf");
    }

    @Test
    void spectatorUsesSpectatorFormat() {
        ArrayList<Player> spectators = new ArrayList<>(List.of(player));
        when(arena.getSpectators()).thenReturn(spectators);
        when(arena.getPlayers()).thenReturn(Set.of());
        when(arena.status()).thenReturn(GameStatus.playing);
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            manager.update(player);
        }
        verify(player).setPlayerListName(ChatColor.GRAY + "Steve");
        verify(player).setPlayerListHeaderFooter("P Diamond II", "wf");
    }

    @Test
    void arenaFieldBeatsConfigDefault() {
        when(arena.getTablistId()).thenReturn("other");
        assertEquals("Other", manager.layoutFor(arena).id());
        when(arena.getTablistId()).thenReturn(null);
        assertEquals("Default", manager.layoutFor(arena).id());
        Properties.DefaultTablist = "Other";
        assertEquals("Other", manager.layoutFor(arena).id());
        when(arena.getTablistId()).thenReturn("Default");
        assertEquals("Default", manager.layoutFor(arena).id());
    }

    @Test
    void unknownLayoutFallsBackToDefaultWithOneWarning() {
        when(arena.getTablistId()).thenReturn("Nope");
        assertEquals("Default", manager.layoutFor(arena).id());
        assertEquals("Default", manager.layoutFor(arena).id());
        assertEquals(1, warnings.size());
    }

    @Test
    void lobbyLayoutAppliesOutsideArenasWhenEnabled() {
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(null);
            manager.update(player);
        }
        verify(player).setPlayerListHeaderFooter("lobby head", "");
        verify(player).setPlayerListName(ChatColor.GRAY + "Steve");
    }

    @Test
    void leavingArenaForDisabledLobbyRestoresVanilla() {
        TablistManager noLobby = new TablistManager(TablistConfig.parse(YamlConfiguration.loadConfiguration(
                new java.io.StringReader(YAML.replace("Enabled: true", "Enabled: false"))), null), null);
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            noLobby.update(player);
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(null);
            noLobby.update(player);
        }
        verify(player).setPlayerListHeaderFooter((String) null, (String) null);
        verify(player).setPlayerListName(null);
    }

    @Test
    void disabledSwitchNeverTouchesPlayers() {
        Properties.TablistEnabled = false;
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(any())).thenReturn(arena);
            manager.update(player);
            reg.when(() -> ArenaRegistry.byWorld(any())).thenReturn(null);
            manager.update(player);
        }
        verify(player, never()).setPlayerListName(any());
        verify(player, never()).setPlayerListHeaderFooter(any(String.class), any(String.class));
    }

    @Test
    void playerOutsideBothListsInArenaWorldIsLeftAlone() {
        when(arena.getPlayers()).thenReturn(Set.of());
        try (MockedStatic<ArenaRegistry> reg = mockStatic(ArenaRegistry.class)) {
            reg.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
            manager.update(player);
        }
        verify(player, never()).setPlayerListName(any());
    }

    @Test
    void rendererFillsPlaceholdersWithoutTeam() {
        when(arena.getTeam(player)).thenReturn(null);
        assertEquals("[]Steve castle", TablistRenderer.render("[{teamShort}]{player} {arena}", player, arena, player));
        assertSame("", TablistRenderer.join(List.of(), player, arena, player));
    }
}
