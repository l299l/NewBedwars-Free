package com.l299l.newbedwars.tablist;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class TablistRendererTest {
    private String savedServerName;
    private Player viewer;
    private Player target;
    private IArena arena;

    @BeforeEach
    void setUp() {
        savedServerName = Properties.ServerName;
        Properties.ServerName = "MyNet";
        viewer = mock(Player.class);
        when(viewer.getName()).thenReturn("Viewer");
        target = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(target.getUniqueId()).thenReturn(id);
        when(target.getName()).thenReturn("Alex");
        arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("castle");
        when(arena.getActivePlayerCount()).thenReturn(5);
        when(arena.getMaxPlayers()).thenReturn(8);
        when(arena.getMinPlayers()).thenReturn(2);
        when(arena.getNextGamePhase()).thenReturn("Bed Gone");
        when(arena.getPhaseTime()).thenReturn(75);
        when(arena.getGameTime()).thenReturn(30);
        GamePlayer gp = mock(GamePlayer.class);
        when(gp.getKills()).thenReturn(4);
        when(gp.getFinalKills()).thenReturn(2);
        when(gp.getBedsBroken()).thenReturn(1);
        when(arena.getPlayer(id)).thenReturn(gp);
        Team team = mock(Team.class);
        when(team.getColor()).thenReturn(ChatColor.BLUE);
        when(team.getDisplayName()).thenReturn("blue");
        when(arena.getTeam(target)).thenReturn(team);
    }

    @AfterEach
    void tearDown() {
        Properties.ServerName = savedServerName;
    }

    @Test
    void arenaPlaceholdersAreFilled() {
        assertEquals("castle 5/8 min 2 Bed Gone 1m 15s 30s",
                TablistRenderer.render("{arena} {players}/{maxPlayers} min {minPlayers} {phase} {phaseTime} {gameTime}", viewer, arena, target));
    }

    @Test
    void playerAndTeamPlaceholdersAreFilled() {
        assertEquals(ChatColor.BLUE + "[B] blue Alex",
                TablistRenderer.render("{teamColor}[{teamShort}] {teamName} {player}", viewer, arena, target));
    }

    @Test
    void statPlaceholdersComeFromTheTarget() {
        assertEquals("4/2/1", TablistRenderer.render("{kills}/{finalKills}/{bedsBroken}", viewer, arena, target));
    }

    @Test
    void statsAreZeroWithoutGamePlayerAndTeamPartsEmptyWithoutTeam() {
        when(arena.getPlayer(target.getUniqueId())).thenReturn(null);
        when(arena.getTeam(target)).thenReturn(null);
        assertEquals("0|0|0|[]|", TablistRenderer.render("{kills}|{finalKills}|{bedsBroken}|[{teamShort}{teamName}]|{teamColor}", viewer, arena, target));
    }

    @Test
    void serverAndOnlineWorkWithoutArena() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(new ArrayList<Player>(List.of(viewer, target)));
            assertEquals("MyNet 2 Alex {arena}", TablistRenderer.render("{server} {online} {player} {arena}", viewer, null, target));
        }
    }

    @Test
    void nullServerNameRendersEmptyAndNullTargetEmptiesPlayer() {
        Properties.ServerName = null;
        assertEquals("[][]", TablistRenderer.render("[{server}][{player}]", viewer, null, null));
    }

    @Test
    void ampersandColorsAreTranslated() {
        assertEquals(ChatColor.GREEN + "hi", TablistRenderer.render("&ahi", viewer, arena, target));
    }

    @Test
    void nullTemplateIsEmpty() {
        assertEquals("", TablistRenderer.render(null, viewer, arena, target));
    }

    @Test
    void joinSeparatesLinesWithNewlinesAndEmptyListIsEmpty() {
        assertEquals("a castle\nb Alex", TablistRenderer.join(List.of("a {arena}", "b {player}"), viewer, arena, target));
        assertEquals("", TablistRenderer.join(null, viewer, arena, target));
        assertSame("", TablistRenderer.join(List.of(), viewer, arena, target));
    }
}
