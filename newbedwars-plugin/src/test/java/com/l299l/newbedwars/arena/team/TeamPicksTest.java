package com.l299l.newbedwars.arena.team;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.l299l.newbedwars.arena.team.TeamPicks.Result;
import static org.junit.jupiter.api.Assertions.*;

class TeamPicksTest {

    private static final List<String> TEAMS = List.of("Red", "Blue", "Green");

    @Test
    void nullTeamIsUnknown() {
        assertEquals(Result.UNKNOWN_TEAM, TeamPicks.canPick(null, TEAMS, 1, Map.of(), 2, false));
    }

    @Test
    void missingTeamIsUnknown() {
        assertEquals(Result.UNKNOWN_TEAM, TeamPicks.canPick("Pink", TEAMS, 1, Map.of(), 2, true));
    }

    @Test
    void fullWhenCountPlusGroupExceedsMax() {
        assertEquals(Result.FULL, TeamPicks.canPick("Red", TEAMS, 1, Map.of("Red", 2), 2, false));
    }

    @Test
    void partyDoesNotFitIntoTeamWithOnePicked() {
        assertEquals(Result.FULL, TeamPicks.canPick("Red", TEAMS, 2, Map.of("Red", 1), 2, false));
    }

    @Test
    void partyFitsExactly() {
        assertEquals(Result.OK, TeamPicks.canPick("Red", TEAMS, 2, Map.of("Red", 0), 2, false));
    }

    @Test
    void fullTakesPriorityOverUnbalanced() {
        assertEquals(Result.FULL, TeamPicks.canPick("Red", TEAMS, 1, Map.of("Red", 2), 2, true));
    }

    @Test
    void unbalancedWhenTargetAboveMinimum() {
        Map<String, Integer> counts = Map.of("Red", 1);
        assertEquals(Result.UNBALANCED, TeamPicks.canPick("Red", TEAMS, 1, counts, 4, true));
    }

    @Test
    void sameInputIsOkWhenNotBalanced() {
        Map<String, Integer> counts = Map.of("Red", 1);
        assertEquals(Result.OK, TeamPicks.canPick("Red", TEAMS, 1, counts, 4, false));
    }

    @Test
    void balancedAllowsTeamAtMinimum() {
        Map<String, Integer> counts = Map.of("Red", 1, "Blue", 1);
        assertEquals(Result.OK, TeamPicks.canPick("Green", TEAMS, 1, counts, 4, true));
    }

    @Test
    void balancedAllowsWhenAllEqual() {
        Map<String, Integer> counts = Map.of("Red", 1, "Blue", 1, "Green", 1);
        assertEquals(Result.OK, TeamPicks.canPick("Blue", TEAMS, 1, counts, 4, true));
    }

    @Test
    void emptyCountsAreOk() {
        assertEquals(Result.OK, TeamPicks.canPick("Blue", TEAMS, 1, Map.of(), 2, true));
    }

    @Test
    void setReplacesPreviousPick() {
        TeamPicks picks = new TeamPicks();
        UUID id = UUID.randomUUID();
        picks.set(id, "Red");
        picks.set(id, "Blue");
        assertEquals("Blue", picks.get(id));
        assertEquals(1, picks.all().size());
    }

    @Test
    void getReturnsNullWithoutPick() {
        assertNull(new TeamPicks().get(UUID.randomUUID()));
    }

    @Test
    void clearReportsWhetherPickExisted() {
        TeamPicks picks = new TeamPicks();
        UUID id = UUID.randomUUID();
        picks.set(id, "Red");
        assertTrue(picks.clear(id));
        assertFalse(picks.clear(id));
        assertNull(picks.get(id));
    }

    @Test
    void clearAllEmptiesPicks() {
        TeamPicks picks = new TeamPicks();
        picks.set(UUID.randomUUID(), "Red");
        picks.set(UUID.randomUUID(), "Blue");
        assertFalse(picks.isEmpty());
        picks.clearAll();
        assertTrue(picks.isEmpty());
        assertTrue(picks.all().isEmpty());
    }

    @Test
    void allIsUnmodifiable() {
        TeamPicks picks = new TeamPicks();
        UUID id = UUID.randomUUID();
        picks.set(id, "Red");
        Map<UUID, String> view = picks.all();
        assertThrows(UnsupportedOperationException.class, () -> view.put(UUID.randomUUID(), "Blue"));
        assertThrows(UnsupportedOperationException.class, view::clear);
    }
}
