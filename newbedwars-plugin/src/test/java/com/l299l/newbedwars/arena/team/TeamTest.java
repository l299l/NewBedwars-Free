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
    private IArena arena;

    @BeforeEach
    void setUp() {
        arena = mock(IArena.class);
        team = new Team(ChatColor.RED, "Red", arena);
    }

    private IArena arenaMock() {
        return arena;
    }

    private Player mockPlayer() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        return player;
    }

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

    @Test
    void colorCanBeChangedOutsideAGame() {
        when(arenaMock().status()).thenReturn(com.l299l.newbedwars.arena.GameStatus.waiting);
        team.setColor(ChatColor.BLUE);
        assertEquals(ChatColor.BLUE, team.getColor());
    }

    @Test
    void colorCanNotBeChangedDuringAGame() {
        when(arenaMock().status()).thenReturn(com.l299l.newbedwars.arena.GameStatus.playing);
        assertThrows(IllegalStateException.class, () -> team.setColor(ChatColor.BLUE));
        assertEquals(ChatColor.RED, team.getColor());
    }

    @Test
    void colorMustBeARealColor() {
        assertThrows(IllegalArgumentException.class, () -> team.setColor(null));
        assertThrows(IllegalArgumentException.class, () -> team.setColor(ChatColor.BOLD));
    }

    @Test
    void teamWithoutBedIsNotBedDestroyed() {
        assertFalse(team.isBedDestroyed());
    }

    @Test
    void aRejoiningPlayerIsCountedOnce() {
        Team team = this.team;
        java.util.UUID id = java.util.UUID.randomUUID();
        Player before = mock(Player.class);
        Player after = mock(Player.class);
        when(before.getUniqueId()).thenReturn(id);
        when(after.getUniqueId()).thenReturn(id);
        team.addPlayer(before);
        team.addPlayer(after);
        assertEquals(1, team.getPlayers().size());
        team.removePlayer(before);
        assertFalse(team.isAlive());
    }

    @Test
    void displayNameFallsBackToTheName() {
        assertEquals("Red", team.getDisplayName());
        team.setDisplayName("  Lime ");
        assertEquals("Lime", team.getDisplayName());
        assertEquals("Red", team.getName());
        team.setDisplayName("");
        assertEquals("Red", team.getDisplayName());
        assertNull(team.getStoredDisplayName());
    }

    @Test
    void invalidDisplayNamesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> team.setDisplayName("\u00a7aLime"));
        assertThrows(IllegalArgumentException.class, () -> team.setDisplayName("Li\nme"));
        assertThrows(IllegalArgumentException.class, () -> team.setDisplayName("x".repeat(33)));
        assertEquals("Red", team.getDisplayName());
    }

    @Test
    void displayNameCanNotChangeDuringAGame() {
        when(arena.status()).thenReturn(com.l299l.newbedwars.arena.GameStatus.playing);
        assertThrows(IllegalStateException.class, () -> team.setDisplayName("Lime"));
    }

    @Test
    void displayNameIsSavedOnlyWhenSet() {
        assertFalse(team.toJson().contains("displayName"));
        team.setDisplayName("Lime \"A\"");
        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(team.toJson()).getAsJsonObject();
        assertEquals("Lime \"A\"", json.get("displayName").getAsString());
        assertEquals("Red", json.get("name").getAsString());
    }
}
