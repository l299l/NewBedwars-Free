package com.l299l.newbedwars.arena.dragon;

import com.l299l.newbedwars.arena.dragon.DragonTargeting.Candidates;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Kind;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Target;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DragonTargetingTest {
    private static final Vector DRAGON = new Vector(0, 80, 0);

    private static Target player(String id, double x, double z) {
        return new Target(id, Kind.PLAYER, new Vector(x, 64, z));
    }

    private static Target bed(String id, double x, double z) {
        return new Target(id, Kind.BED, new Vector(x, 64, z));
    }

    @Test
    void enemyNearABedComesFirst() {
        Target near = player("near", 5, 0);
        Target atBed = player("atBed", 60, 0);
        Candidates c = new Candidates(List.of(near, atBed), List.of(), List.of(), List.of(new Vector(62, 64, 0)));
        assertEquals("atBed", DragonTargeting.pick(DRAGON, c, 128).id());
    }

    @Test
    void nearestEnemyWhenNobodyIsNearABed() {
        Candidates c = new Candidates(List.of(player("far", 60, 0), player("near", 5, 0)),
                List.of(bed("b", 0, 30)), List.of(), List.of(new Vector(0, 64, -100)));
        assertEquals("near", DragonTargeting.pick(DRAGON, c, 128).id());
    }

    @Test
    void bedWhenNoPlayersThenDragon() {
        Target dragon = new Target("d", Kind.DRAGON, new Vector(10, 80, 0));
        Candidates beds = new Candidates(List.of(), List.of(bed("far", 50, 0), bed("near", 20, 0)), List.of(dragon), List.of());
        assertEquals("near", DragonTargeting.pick(DRAGON, beds, 128).id());
        Candidates onlyDragon = new Candidates(List.of(), List.of(), List.of(dragon), List.of());
        assertEquals(Kind.DRAGON, DragonTargeting.pick(DRAGON, onlyDragon, 128).kind());
        assertNull(DragonTargeting.pick(DRAGON, Candidates.NONE, 128));
    }

    @Test
    void targetsOutOfRangeAreIgnored() {
        Candidates c = new Candidates(List.of(player("far", 200, 0)), List.of(), List.of(), List.of());
        assertNull(DragonTargeting.pick(DRAGON, c, 50));
    }

    @Test
    void keepsTheCurrentTargetWhileItIsValid() {
        Target a = player("a", 30, 0);
        Target b = player("b", 5, 0);
        Candidates c = new Candidates(List.of(a, b), List.of(), List.of(), List.of());
        assertEquals("a", DragonTargeting.choose(a, DRAGON, c, 128, false).id());
        assertEquals("b", DragonTargeting.choose(a, DRAGON, c, 128, true).id());
    }

    @Test
    void currentTargetGetsFreshPosition() {
        Target old = player("a", 30, 0);
        Target moved = player("a", 32, 4);
        Candidates c = new Candidates(List.of(moved), List.of(), List.of(), List.of());
        assertEquals(moved.position(), DragonTargeting.choose(old, DRAGON, c, 128, false).position());
    }

    @Test
    void picksAnotherTargetWhenTheCurrentOneIsGone() {
        Target gone = player("gone", 3, 0);
        Candidates c = new Candidates(List.of(player("other", 40, 0)), List.of(), List.of(), List.of());
        assertEquals("other", DragonTargeting.choose(gone, DRAGON, c, 128, false).id());
        assertNull(DragonTargeting.choose(gone, DRAGON, Candidates.NONE, 128, false));
    }
}
