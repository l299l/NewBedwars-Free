package com.l299l.newbedwars.arena.dragon;

import com.l299l.newbedwars.arena.dragon.DragonBrain.State;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Candidates;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Kind;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Target;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DragonBrainTest {
    private static final DragonBounds BOUNDS = DragonBounds.of(List.of(
            new Vector(-50, 64, 0), new Vector(50, 64, 0),
            new Vector(0, 64, -50), new Vector(0, 64, 50)), List.of(), 10, -64, 320, 15, 0);
    private static final DragonSettings SETTINGS = DragonSettings.DEFAULTS;

    private static Candidates players(Target... targets) {
        return new Candidates(List.of(targets), List.of(), List.of(), List.of());
    }

    private static int runUntil(DragonBrain brain, Candidates c, State wanted, int maxTicks) {
        for (int i = 1; i <= maxTicks; i++) {
            brain.tick(BOUNDS, c, SETTINGS);
            if (brain.state() == wanted) return i;
        }
        return -1;
    }

    @Test
    void returnsToPatrolWithoutTargets() {
        DragonBrain brain = new DragonBrain(new Vector(-50, 80, 0), BOUNDS, 400);
        assertTrue(runUntil(brain, Candidates.NONE, State.PATROL, 400) > 0);
        for (int i = 0; i < 400; i++) {
            brain.tick(BOUNDS, Candidates.NONE, SETTINGS);
            assertEquals(State.PATROL, brain.state());
        }
    }

    @Test
    void patrolKeepsDistanceFromTheCenter() {
        DragonBrain brain = new DragonBrain(new Vector(-50, 80, 0), BOUNDS, 400);
        runUntil(brain, Candidates.NONE, State.PATROL, 400);
        for (int i = 0; i < 600; i++) {
            brain.tick(BOUNDS, Candidates.NONE, SETTINGS);
            Vector p = brain.position();
            double radius = Math.hypot(p.getX(), p.getZ());
            assertTrue(radius > BOUNDS.patrolRadius() * 0.6 && radius < BOUNDS.patrolRadius() * 1.4, "radius " + radius);
        }
    }

    @Test
    void seeksAndAttacksATarget() {
        DragonBrain brain = new DragonBrain(new Vector(-50, 80, 0), BOUNDS, 400);
        Candidates c = players(new Target("p", Kind.PLAYER, new Vector(30, 64, 30)));
        brain.tick(BOUNDS, c, SETTINGS);
        assertEquals(State.SEEK, brain.state());
        assertTrue(runUntil(brain, c, State.ATTACK, 600) > 0);
        assertTrue(runUntil(brain, c, State.CLIMB, 200) > 0);
        assertTrue(runUntil(brain, c, State.SEEK, 100) > 0);
    }

    @Test
    void goesBackWhenTheTargetDisappears() {
        DragonBrain brain = new DragonBrain(new Vector(-50, 80, 0), BOUNDS, 400);
        brain.tick(BOUNDS, players(new Target("p", Kind.PLAYER, new Vector(30, 64, 30))), SETTINGS);
        assertEquals(State.SEEK, brain.state());
        brain.tick(BOUNDS, Candidates.NONE, SETTINGS);
        assertEquals(State.RETURN, brain.state());
        assertNull(brain.target());
    }

    @Test
    void firesOnlyAfterTheCooldown() {
        DragonBrain brain = new DragonBrain(new Vector(0, 80, 0), BOUNDS, 100);
        Candidates c = players(new Target("p", Kind.PLAYER, new Vector(0, 64, 10)));
        int shots = 0;
        int lastShot = -1000;
        for (int i = 0; i < 1000; i++) {
            if (brain.tick(BOUNDS, c, SETTINGS) != null) {
                shots++;
                assertTrue(i - lastShot >= 100, "shots too close: " + (i - lastShot));
                lastShot = i;
            }
        }
        assertTrue(shots >= 5, "shots " + shots);
    }

    @Test
    void neverLeavesTheBounds() {
        Random random = new Random(42);
        DragonBrain brain = new DragonBrain(new Vector(500, 300, -500), BOUNDS, 100);
        Candidates c = Candidates.NONE;
        for (int i = 0; i < 5000; i++) {
            if (i % 150 == 0) {
                c = random.nextInt(3) == 0 ? Candidates.NONE
                        : players(new Target("p" + i, Kind.PLAYER,
                        new Vector(random.nextInt(120) - 60, 64, random.nextInt(120) - 60)));
            }
            brain.tick(BOUNDS, c, SETTINGS);
            assertTrue(BOUNDS.contains(brain.position()), "left the bounds at " + brain.position());
        }
    }

    @Test
    void movesAtMostOneStepPerTick() {
        DragonBrain brain = new DragonBrain(new Vector(-50, 80, 0), BOUNDS, 100);
        Candidates c = players(new Target("p", Kind.PLAYER, new Vector(30, 64, 30)));
        Vector last = brain.position();
        double max = SETTINGS.speed() * 1.25 + 1e-9;
        for (int i = 0; i < 500; i++) {
            brain.tick(BOUNDS, c, SETTINGS);
            assertTrue(brain.position().distance(last) <= max);
            last = brain.position();
        }
    }

    @Test
    void settingsFallBackToDefaults() {
        DragonSettings s = DragonSettings.read(Map.<String, Object>of(
                "Dragon.Speed", 1.2,
                "Dragon.TurnRate", -3,
                "Dragon.FireballIntervalSeconds", List.of(30, "x", 0, 5))::get);
        assertEquals(1.2, s.speed());
        assertEquals(DragonSettings.DEFAULTS.turnRate(), s.turnRate());
        assertEquals(List.of(30, 5), s.fireballIntervalSeconds());
        assertEquals(600, s.fireballIntervalTicks(1));
        assertEquals(100, s.fireballIntervalTicks(2));
        assertEquals(100, s.fireballIntervalTicks(7));
        assertEquals(600, s.fireballIntervalTicks(0));
        assertEquals(DragonSettings.DEFAULTS, DragonSettings.read(key -> null));
    }
}
