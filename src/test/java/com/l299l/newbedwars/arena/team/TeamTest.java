package com.l299l.newbedwars.arena.team;

import com.l299l.newbedwars.arena.IArena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamTest {

    private Team team;

    @BeforeEach
    void setUp() {
        IArena arena = mock(IArena.class);
        team = new Team(ChatColor.RED, "Red", arena);
    }

    private Player mockPlayer() {
        return mock(Player.class);
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    void newTeamHasCorrectNameAndColor() {
        assertEquals("Red", team.getName());
        assertEquals(ChatColor.RED, team.getColor());
    }

    @Test
    void newTeamIsAlive() {
        assertTrue(team.isAlive());
    }

    @Test
    void newTeamHasNoPlayers() {
        assertTrue(team.getPlayers().isEmpty());
    }

    // ── addPlayer / removePlayer ──────────────────────────────────────────────

    @Test
    void addPlayerIncreasesPlayerCount() {
        team.addPlayer(mockPlayer());
        assertEquals(1, team.getPlayers().size());
    }

    @Test
    void addPlayerKeepsTeamAlive() {
        team.addPlayer(mockPlayer());
        assertTrue(team.isAlive());
    }

    @Test
    void removeLastPlayerSetsTeamDead() {
        Player p = mockPlayer();
        team.addPlayer(p);
        team.removePlayer(p);
        assertFalse(team.isAlive());
    }

    @Test
    void removeOneOfTwoPlayersTeamRemainsAlive() {
        Player p1 = mockPlayer();
        Player p2 = mockPlayer();
        team.addPlayer(p1);
        team.addPlayer(p2);
        team.removePlayer(p1);
        assertTrue(team.isAlive());
        assertEquals(1, team.getPlayers().size());
    }

    @Test
    void removeNonMemberIsNoOp() {
        Player member = mockPlayer();
        Player outsider = mockPlayer();
        team.addPlayer(member);
        team.removePlayer(outsider);
        assertEquals(1, team.getPlayers().size());
        assertTrue(team.isAlive());
    }

    // ── isInBuildProtArea ─────────────────────────────────────────────────────

    private static Location loc(double x, double y, double z) {
        return new Location(null, x, y, z);
    }

    @Test
    void isInBuildProtAreaReturnsFalseWhenNotSet() {
        assertFalse(team.isInBuildProtArea(loc(0, 64, 0)));
    }

    @Test
    void isInBuildProtAreaReturnsFalseWhenOnlyPos1Set() {
        team.setTeamBuildProtAreaPos1(loc(0, 60, 0));
        assertFalse(team.isInBuildProtArea(loc(0, 64, 0)));
    }

    @Test
    void isInBuildProtAreaReturnsTrueForLocationInsideBox() {
        team.setTeamBuildProtAreaPos1(loc(0, 60, 0));
        team.setTeamBuildProtAreaPos2(loc(10, 70, 10));
        assertTrue(team.isInBuildProtArea(loc(5, 65, 5)));
    }

    @Test
    void isInBuildProtAreaReturnsFalseForLocationOutsideBox() {
        team.setTeamBuildProtAreaPos1(loc(0, 60, 0));
        team.setTeamBuildProtAreaPos2(loc(10, 70, 10));
        assertFalse(team.isInBuildProtArea(loc(20, 65, 5)));
    }

    @Test
    void isInBuildProtAreaHandlesInvertedCorners() {
        team.setTeamBuildProtAreaPos1(loc(10, 70, 10));
        team.setTeamBuildProtAreaPos2(loc(0, 60, 0));
        assertTrue(team.isInBuildProtArea(loc(5, 65, 5)));
    }

    @Test
    void isInBuildProtAreaReturnsTrueOnBoundary() {
        team.setTeamBuildProtAreaPos1(loc(0, 60, 0));
        team.setTeamBuildProtAreaPos2(loc(10, 70, 10));
        assertTrue(team.isInBuildProtArea(loc(0, 60, 0)));
        assertTrue(team.isInBuildProtArea(loc(10, 70, 10)));
    }

    // ── isInBase ─────────────────────────────────────────────────────────────

    @Test
    void isInBaseReturnsFalseWhenNotSet() {
        assertFalse(team.isInBase(loc(5, 64, 5)));
    }

    @Test
    void isInBaseReturnsTrueForLocationInsideBase() {
        team.setTeamBasePos1(loc(-5, 58, -5));
        team.setTeamBasePos2(loc(15, 75, 15));
        assertTrue(team.isInBase(loc(5, 65, 5)));
    }

    // ── null checks for optional fields ───────────────────────────────────────

    @Test
    void isBedSetReturnsFalseInitially() {
        assertFalse(team.isBedSet());
    }

    @Test
    void isSpawnSetReturnsFalseInitially() {
        assertFalse(team.isSpawnSet());
    }

    @Test
    void buildProtAreaPosSetReturnsFalseWhenNeitherSet() {
        assertFalse(team.isBuildProtAreaPosSet());
    }

    @Test
    void buildProtAreaPosSetReturnsTrueWhenBothSet() {
        team.setTeamBuildProtAreaPos1(loc(0, 60, 0));
        team.setTeamBuildProtAreaPos2(loc(10, 70, 10));
        assertTrue(team.isBuildProtAreaPosSet());
    }
}
