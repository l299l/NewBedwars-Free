package com.l299l.newbedwars.tablist;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.team.TeamPicks;
import com.l299l.newbedwars.arena.team.TeamSelectorManager;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private TeamSelectorManager previousSelector;

    private void restoreSelector() throws Exception {
        Field f = TeamSelectorManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, previousSelector);
    }

    private String renderPicked(boolean selection, GameStatus status, boolean assigned, boolean picked) throws Exception {
        previousSelector = TeamSelectorManager.get();
        try {
            new TeamSelectorManager();
            Gamerules rules = mock(Gamerules.class);
            rules.TeamSelection = selection;
            when(arena.getGamerules()).thenReturn(rules);
            when(arena.status()).thenReturn(status);
            Team red = mock(Team.class);
            when(red.getColor()).thenReturn(ChatColor.RED);
            when(red.getDisplayName()).thenReturn("red");
            Map<String, Team> teams = new LinkedHashMap<>();
            teams.put("red", red);
            when(arena.getTeams()).thenReturn(teams);
            if (!assigned) when(arena.getTeam(target)).thenReturn(null);
            TeamPicks picks = new TeamPicks();
            if (picked) picks.set(target.getUniqueId(), "red");
            when(arena.getTeamPicks()).thenReturn(picks);
            when(arena.isPlayerInArena(target)).thenReturn(true);
            when(arena.pickGroup(target)).thenReturn(List.of(target));
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(target.getUniqueId())).thenReturn(target);
                return TablistRenderer.render("{teamColor}[{teamShort}]{teamName}", viewer, arena, target);
            }
        } finally {
            restoreSelector();
        }
    }

    @Test
    void pickedTeamFillsTeamPlaceholdersWhileWaiting() throws Exception {
        assertEquals(ChatColor.RED + "[R]red", renderPicked(true, GameStatus.waiting, false, true));
        assertEquals(ChatColor.RED + "[R]red", renderPicked(true, GameStatus.starting, false, true));
    }

    @Test
    void noPickLeavesTeamPlaceholdersEmpty() throws Exception {
        assertEquals("[]", renderPicked(true, GameStatus.waiting, false, false));
    }

    @Test
    void pickIsIgnoredWhenTheTeamPickerIsOffOrTheGameRuns() throws Exception {
        assertEquals("[]", renderPicked(false, GameStatus.waiting, false, true));
        assertEquals("[]", renderPicked(true, GameStatus.playing, false, true));
    }

    @Test
    void assignedTeamWinsOverThePick() throws Exception {
        assertEquals(ChatColor.BLUE + "[B]blue", renderPicked(true, GameStatus.waiting, true, true));
    }
}
