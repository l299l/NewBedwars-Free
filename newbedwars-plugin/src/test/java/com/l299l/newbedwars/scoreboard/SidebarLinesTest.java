package com.l299l.newbedwars.scoreboard;

import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SidebarLinesTest {
    private static final String HEX = "§x§0§E§D§6§A§A";

    private Scoreboard board;
    private Objective objective;
    private Team team;
    private SidebarLines sidebar;

    @BeforeEach
    void setUp() {
        board = mock(Scoreboard.class);
        objective = mock(Objective.class);
        team = mock(Team.class);
        when(board.getObjective("nbw")).thenReturn(objective);
        when(objective.getScore(anyString())).thenReturn(mock(Score.class));
        when(board.registerNewTeam(anyString())).thenReturn(team);
        sidebar = new SidebarLines("nbw", board);
    }

    @Test
    void hexLineGoesToTeamPrefix() {
        String line = HEX + "Event";
        sidebar.apply(List.of(line));
        verify(board).registerNewTeam("nbw_l0");
        verify(team).addEntry(SidebarLines.teamEntry(0));
        verify(team).setPrefix(line);
        verify(team).setSuffix("");
        verify(objective).getScore(SidebarLines.teamEntry(0));
        verify(objective, never()).getScore(line);
    }

    @Test
    void plainShortLineStaysAnEntry() {
        sidebar.apply(List.of("Map castle"));
        verify(board, never()).registerNewTeam(anyString());
        verify(objective).getScore("Map castle");
    }

    @Test
    void longLineBelow1203IsNotCut() {
        String line = "a".repeat(60);
        try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getBukkitVersion).thenReturn("1.17.1-R0.1-SNAPSHOT");
            sidebar.apply(List.of(line));
        }
        verify(team).setPrefix(line);
        verify(objective, never()).getScore(line);
    }

    @Test
    void longLineOn1203StaysAnEntry() {
        String line = "a".repeat(60);
        try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getBukkitVersion).thenReturn("1.21.4-R0.1-SNAPSHOT");
            sidebar.apply(List.of(line));
        }
        verify(board, never()).registerNewTeam(anyString());
        verify(objective).getScore(line);
    }

    @Test
    void switchingRowsLeavesNothingBehind() {
        sidebar.apply(List.of("plain"));
        sidebar.apply(List.of(HEX + "now"));
        verify(board).resetScores("plain");
        sidebar.apply(List.of("plain"));
        verify(board).resetScores(SidebarLines.teamEntry(0));
        verify(team).unregister();
    }

    @Test
    void removedHexLineUnregistersItsTeam() {
        sidebar.apply(List.of("a", HEX + "b"));
        sidebar.apply(List.of("a"));
        verify(board).resetScores(SidebarLines.teamEntry(1));
        verify(team).unregister();
    }

    @Test
    void unchangedHexLineIsNotResent() {
        sidebar.apply(List.of(HEX + "x"));
        sidebar.apply(List.of(HEX + "x"));
        verify(team, times(1)).setPrefix(anyString());
        verify(board, times(1)).registerNewTeam(anyString());
    }

    @Test
    void duplicateHexLinesGetOwnEntries() {
        sidebar.apply(List.of(HEX + "x", HEX + "x"));
        verify(board).registerNewTeam("nbw_l0");
        verify(board).registerNewTeam("nbw_l1");
        verify(objective).getScore(SidebarLines.teamEntry(0));
        verify(objective).getScore(SidebarLines.teamEntry(1));
    }

    @Test
    void clearUnregistersTeams() {
        sidebar.apply(List.of(HEX + "x"));
        sidebar.clear();
        verify(team).unregister();
    }

    @Test
    void packerSplitsAndCarriesTheColor() {
        LinePacker.Packed packed = LinePacker.pack(HEX + "a".repeat(70));
        assertEquals(64, packed.prefix().length());
        assertTrue(packed.suffix().startsWith(HEX));
        long letters = (packed.prefix() + packed.suffix()).chars().filter(c -> c == 'a').count();
        assertEquals(70, letters);
    }

    @Test
    void packerNeverSplitsAHexSequence() {
        LinePacker.Packed packed = LinePacker.pack("a".repeat(55) + HEX + "tail");
        assertEquals("a".repeat(55), packed.prefix());
        assertTrue(packed.suffix().startsWith(HEX + "tail"));
    }

    @Test
    void packerKeepsShortLinesInThePrefix() {
        LinePacker.Packed packed = LinePacker.pack(HEX + "Hi");
        assertEquals(HEX + "Hi", packed.prefix());
        assertEquals("", packed.suffix());
    }
}
