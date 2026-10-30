package com.l299l.newbedwars.arena.group;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MapVoteTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();

    @Test
    void mostVotesWins() {
        MapVote v = new MapVote();
        v.vote(a, "Castle");
        v.vote(b, "castle");
        v.vote(c, "Ruins");
        assertEquals(2, v.votes("CASTLE"));
        assertEquals("Castle", v.winner(List.of("Ruins", "Castle"), new Random(1)));
    }

    @Test
    void changingAVoteReplacesIt() {
        MapVote v = new MapVote();
        v.vote(a, "Castle");
        v.vote(a, "Ruins");
        assertEquals(0, v.votes("Castle"));
        assertEquals(1, v.votes("Ruins"));
        assertEquals("ruins", v.voteOf(a));
    }

    @Test
    void tieAndNoVotesPickRandomlyAmongTied() {
        MapVote v = new MapVote();
        v.vote(a, "Castle");
        v.vote(b, "Ruins");
        Set<String> seen = new HashSet<>();
        Random random = new Random(42);
        for (int i = 0; i < 50; i++) seen.add(v.winner(List.of("Castle", "Ruins", "Desert"), random));
        assertEquals(Set.of("Castle", "Ruins"), seen, "Desert has fewer votes and never wins");

        MapVote empty = new MapVote();
        Set<String> any = new HashSet<>();
        for (int i = 0; i < 50; i++) any.add(empty.winner(List.of("Castle", "Ruins"), random));
        assertEquals(Set.of("Castle", "Ruins"), any);
    }

    @Test
    void votesForUnavailableMapsDoNotCount() {
        MapVote v = new MapVote();
        v.vote(a, "Gone");
        v.vote(b, "Gone");
        v.vote(c, "Castle");
        assertEquals("Castle", v.winner(List.of("Castle"), new Random()));
        v.removeOption("gone");
        assertNull(v.voteOf(a));
        assertEquals(0, v.votes("Gone"));
    }

    @Test
    void leavingRemovesTheVote() {
        MapVote v = new MapVote();
        v.vote(a, "Castle");
        v.removeVoter(a);
        assertEquals(0, v.votes("Castle"));
    }

    @Test
    void closedVoteRejectsVotes() {
        MapVote v = new MapVote();
        v.vote(a, "Castle");
        v.close();
        assertTrue(v.isClosed());
        assertFalse(v.vote(b, "Ruins"));
        assertEquals(0, v.votes("Ruins"));
        assertEquals(1, v.votes("Castle"));
    }

    @Test
    void noOptionsNoWinner() {
        assertNull(new MapVote().winner(List.of(), new Random()));
    }
}
