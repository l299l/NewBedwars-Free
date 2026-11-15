package com.l299l.newbedwars.arena.setup.auto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AreaGuesserTest {

    @Test
    void baseIsTheBoundsOfAllPointsWithPadding() {
        Box box = AreaGuesser.base(List.of(new Pos(0, 64, 0), new Pos(10, 66, -4)), new AreaGuesser.Padding(8, 5, 15));
        assertEquals(new Pos(-8, 59, -12), box.min());
        assertEquals(new Pos(18, 81, 8), box.max());
    }

    @Test
    void baseIgnoresMissingPointsAndIsNullWithoutAny() {
        assertNull(AreaGuesser.base(List.of(), new AreaGuesser.Padding(1, 1, 1)));
        Box box = AreaGuesser.base(Arrays.asList(null, new Pos(5, 5, 5)), new AreaGuesser.Padding(1, 1, 1));
        assertEquals(new Pos(4, 4, 4), box.min());
    }

    @Test
    void protectionSurroundsSpawnAndGeneratorBy3() {
        Box box = AreaGuesser.buildProtection(new Pos(0, 64, 0), new Pos(6, 64, 2), List.of());
        assertEquals(new Pos(-3, 61, -3), box.min());
        assertEquals(new Pos(9, 67, 5), box.max());
    }

    @Test
    void protectionFallsBackToOtherPoints() {
        Box box = AreaGuesser.buildProtection(null, null, List.of(new Pos(10, 70, 10)));
        assertEquals(new Pos(7, 67, 7), box.min());
        assertNull(AreaGuesser.buildProtection(null, null, List.of()));
    }
}
