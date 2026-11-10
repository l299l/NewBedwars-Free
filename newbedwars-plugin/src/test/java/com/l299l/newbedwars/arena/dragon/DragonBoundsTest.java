package com.l299l.newbedwars.arena.dragon;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DragonBoundsTest {

    private static final List<Vector> MAP = List.of(
            new Vector(-50, 64, 0), new Vector(50, 64, 0),
            new Vector(0, 70, -50), new Vector(0, 66, 50));

    @Test
    void emptyPointsGiveNoBounds() {
        assertNull(DragonBounds.of(List.of(), List.of(), 10, -64, 320, 15, 0));
        assertNull(DragonBounds.of(null, null, 10, -64, 320, 15, 0));
    }

    @Test
    void boxIsPointsPlusMargin() {
        DragonBounds b = DragonBounds.of(MAP, List.of(), 10, -64, 320, 15, 0);
        assertTrue(b.contains(new Vector(59, 80, 0)));
        assertFalse(b.contains(new Vector(61, 80, 0)));
        assertTrue(b.contains(new Vector(0, 80, -59)));
        assertFalse(b.contains(new Vector(0, 80, -61)));
    }

    @Test
    void heightLimitsFollowTheMap() {
        DragonBounds b = DragonBounds.of(MAP, List.of(), 10, -64, 320, 15, 0);
        assertEquals(67, b.minY());
        assertEquals(85, b.patrolY());
        assertEquals(105, b.maxY());
    }

    @Test
    void ceilingStaysBelowWorldMax() {
        DragonBounds b = DragonBounds.of(MAP, List.of(), 10, 0, 90, 30, 0);
        assertEquals(85, b.maxY());
        assertTrue(b.patrolY() <= b.maxY());
        assertTrue(b.patrolY() >= b.minY());
    }

    @Test
    void clampKeepsPointsInside() {
        DragonBounds b = DragonBounds.of(MAP, List.of(), 10, -64, 320, 15, 0);
        Vector clamped = b.clamp(new Vector(500, -100, -500));
        assertEquals(60, clamped.getX());
        assertEquals(67, clamped.getY());
        assertEquals(-60, clamped.getZ());
        assertTrue(b.contains(clamped));
    }

    @Test
    void centerIsAverageOfPoints() {
        DragonBounds b = DragonBounds.of(MAP, List.of(), 10, -64, 320, 15, 0);
        assertEquals(0, b.center().getX(), 1e-9);
        assertEquals(0, b.center().getZ(), 1e-9);
    }

    @Test
    void autoPatrolRadiusIsAShareOfTheSmallerSide() {
        DragonBounds auto = DragonBounds.of(MAP, List.of(), 10, -64, 320, 15, 0);
        assertEquals(30, auto.patrolRadius(), 1e-9);
        DragonBounds fixed = DragonBounds.of(MAP, List.of(), 10, -64, 320, 15, 30);
        assertEquals(30, fixed.patrolRadius());
        DragonBounds single = DragonBounds.of(List.of(new Vector(0, 64, 0)), List.of(), 10, -64, 320, 15, 0);
        assertTrue(single.patrolRadius() > 0);
    }

    @Test
    void patrolPointsAreInsideAndAtPatrolHeight() {
        DragonBounds b = DragonBounds.of(MAP, List.of(), 10, -64, 320, 15, 0);
        for (int i = 0; i < 36; i++) {
            Vector p = b.patrolPoint(Math.toRadians(i * 10));
            assertTrue(b.contains(p));
            assertEquals(b.patrolY(), p.getY(), 1e-9);
        }
    }

    @Test
    void baseCornersWidenTheBoxButNotTheHeight() {
        List<Vector> bases = List.of(new Vector(-90, 120, 0), new Vector(0, 20, 90));
        DragonBounds b = DragonBounds.of(MAP, bases, 10, -64, 320, 15, 0);
        assertTrue(b.contains(new Vector(-99, 80, 0)));
        assertTrue(b.contains(new Vector(0, 80, 99)));
        assertEquals(67, b.minY());
        assertEquals(85, b.patrolY());
        assertEquals(105, b.maxY());
        assertEquals(30, b.patrolRadius(), 1e-9);
    }

    @Test
    void targetsMayBeSlightlyBelowTheFloor() {
        DragonBounds b = DragonBounds.of(MAP, List.of(), 10, -64, 320, 15, 0);
        assertTrue(b.holdsTarget(new Vector(0, 64, 0)));
        assertFalse(b.holdsTarget(new Vector(0, 20, 0)));
        assertFalse(b.holdsTarget(new Vector(100, 64, 0)));
    }
}
