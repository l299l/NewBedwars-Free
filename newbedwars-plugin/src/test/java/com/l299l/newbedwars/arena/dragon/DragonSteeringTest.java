package com.l299l.newbedwars.arena.dragon;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DragonSteeringTest {

    private static double angle(Vector a, Vector b) {
        return Math.toDegrees(a.clone().normalize().angle(b.clone().normalize()));
    }

    @Test
    void smallTurnReachesTheDesiredDirection() {
        Vector out = DragonSteering.turn(new Vector(1, 0, 0), new Vector(1, 0, 0.05), Math.toRadians(10));
        assertEquals(0, angle(out, new Vector(1, 0, 0.05)), 1e-6);
    }

    @Test
    void bigTurnIsLimited() {
        Vector from = new Vector(1, 0, 0);
        Vector out = DragonSteering.turn(from, new Vector(0, 0, 1), Math.toRadians(10));
        assertEquals(10, angle(from, out), 1e-6);
        assertTrue(angle(out, new Vector(0, 0, 1)) < 90);
        assertEquals(1, out.length(), 1e-9);
    }

    @Test
    void oppositeDirectionStillTurns() {
        Vector from = new Vector(1, 0, 0);
        Vector out = DragonSteering.turn(from, new Vector(-1, 0, 0), Math.toRadians(10));
        assertEquals(10, angle(from, out), 1e-6);
        Vector up = DragonSteering.turn(new Vector(0, 1, 0), new Vector(0, -1, 0), Math.toRadians(10));
        assertEquals(10, angle(new Vector(0, 1, 0), up), 1e-6);
    }

    @Test
    void headingKeepsDirectionAtTheGoal() {
        Vector pos = new Vector(5, 5, 5);
        Vector dir = DragonSteering.heading(pos, pos.clone(), new Vector(0, 0, 2), 10);
        assertEquals(0, angle(dir, new Vector(0, 0, 1)), 1e-6);
        assertEquals(1, dir.length(), 1e-9);
    }

    @Test
    void dragonYawFacesTheModelForward() {
        assertEquals(180, Math.abs(DragonSteering.yaw(new Vector(0, 0, 1))), 1e-4);
        assertEquals(0, DragonSteering.yaw(new Vector(0, 0, -1)), 1e-4);
        assertEquals(-90, DragonSteering.yaw(new Vector(-1, 0, 0)), 1e-4);
        assertEquals(90, DragonSteering.yaw(new Vector(1, 0, 0)), 1e-4);
    }

    @Test
    void leadAimsAheadOfAMovingTarget() {
        Vector from = new Vector(0, 0, 0);
        Vector target = new Vector(10, 0, 0);
        Vector aim = DragonSteering.lead(from, target, new Vector(0, 0, 0.5), 1.0);
        assertEquals(10, aim.getX(), 1e-9);
        assertEquals(5, aim.getZ(), 1e-9);
        assertEquals(target, DragonSteering.lead(from, target, new Vector(), 1.0));
    }
}
