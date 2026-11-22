package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaderboardPlaceholdersTest {
    private PlayerManager players;

    private static Map.Entry<String, PlayerStats> row(String name, int wins, int finalKills) {
        return new AbstractMap.SimpleImmutableEntry<>(name, new PlayerStats(wins, 0, 0, 0, finalKills, 0, 0));
    }

    @BeforeEach
    void setUp() {
        players = mock(PlayerManager.class);
        when(players.getTopByWins(anyInt())).thenReturn(List.of(row("Alex", 12, 0), row("Steve", 7, 0)));
        when(players.getTopByFinalKills(anyInt())).thenReturn(List.of(row("Steve", 0, 30)));
        when(players.getTopByKills(anyInt())).thenReturn(List.of());
        when(players.getTopByBeds(anyInt())).thenReturn(null);
        LeaderboardPlaceholders.refresh(players);
    }

    @AfterEach
    void tearDown() {
        LeaderboardPlaceholders.clear();
    }

    @Test
    void namesAndValuesByPosition() {
        assertEquals("Alex", LeaderboardPlaceholders.resolve("wins_1_name", "-"));
        assertEquals("12", LeaderboardPlaceholders.resolve("wins_1_value", "-"));
        assertEquals("Steve", LeaderboardPlaceholders.resolve("wins_2_name", "-"));
        assertEquals("Steve", LeaderboardPlaceholders.resolve("final_kills_1_name", "-"));
        assertEquals("30", LeaderboardPlaceholders.resolve("final_kills_1_value", "-"));
    }

    @Test
    void emptyPlacesShowTheFiller() {
        assertEquals("-", LeaderboardPlaceholders.resolve("wins_3_name", "-"));
        assertEquals("0", LeaderboardPlaceholders.resolve("wins_10_value", "-"));
        assertEquals("-", LeaderboardPlaceholders.resolve("kills_1_name", "-"));
        assertEquals("0", LeaderboardPlaceholders.resolve("beds_1_value", "-"));
    }

    @Test
    void unknownPlaceholdersAreLeftAlone() {
        assertNull(LeaderboardPlaceholders.resolve("wins_11_name", "-"));
        assertNull(LeaderboardPlaceholders.resolve("wins_0_name", "-"));
        assertNull(LeaderboardPlaceholders.resolve("wins_x_name", "-"));
        assertNull(LeaderboardPlaceholders.resolve("wins_1_uuid", "-"));
        assertNull(LeaderboardPlaceholders.resolve("deaths_1_name", "-"));
        assertNull(LeaderboardPlaceholders.resolve("name", "-"));
        assertNull(LeaderboardPlaceholders.resolve("_1_name", "-"));
    }

    @Test
    void refreshReplacesTheSnapshot() {
        when(players.getTopByWins(anyInt())).thenReturn(List.of(row("Notch", 99, 0)));
        assertEquals("Alex", LeaderboardPlaceholders.resolve("wins_1_name", "-"));
        LeaderboardPlaceholders.refresh(players);
        assertEquals("Notch", LeaderboardPlaceholders.resolve("wins_1_name", "-"));
        assertEquals("-", LeaderboardPlaceholders.resolve("wins_2_name", "-"));
    }
}
