package com.l299l.newbedwars.arena.dragon;

import com.l299l.newbedwars.arena.dragon.DragonTargeting.Candidates;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Target;
import org.bukkit.util.Vector;

public final class DragonBrain {
    public enum State { PATROL, SEEK, ATTACK, CLIMB, RETURN }

    static final int REEVALUATE_TICKS = 100;
    static final int MIN_SEEK_TICKS = 60;
    static final int MAX_ATTACK_TICKS = 60;
    static final int CLIMB_TICKS = 40;
    static final int FIRST_FIREBALL_TICKS = 40;
    static final double SEEK_ABOVE = 10;
    static final double ATTACK_ABOVE = 1.5;
    static final double HIT_DISTANCE = 5;
    static final double PATROL_REACHED = 8;
    static final double PATROL_LEAD = 0.35;

    private final int fireballInterval;
    private Vector position;
    private Vector direction;
    private State state = State.RETURN;
    private Target target;
    private double patrolAngle;
    private int stateTicks;
    private int ticks;
    private int cooldown = FIRST_FIREBALL_TICKS;

    public DragonBrain(Vector position, DragonBounds bounds, int fireballIntervalTicks) {
        this.position = bounds.clamp(position);
        this.fireballInterval = Math.max(1, fireballIntervalTicks);
        this.patrolAngle = bounds.angleOf(this.position);
        Vector toCenter = bounds.center().subtract(this.position).setY(0);
        this.direction = toCenter.lengthSquared() < 1.0E-6 ? new Vector(1, 0, 0) : toCenter.normalize();
    }

    public Target tick(DragonBounds bounds, Candidates candidates, DragonSettings s) {
        ticks++;
        stateTicks++;
        if (cooldown > 0) cooldown--;
        boolean reevaluate = state == State.PATROL || state == State.RETURN || ticks % REEVALUATE_TICKS == 0;
        target = DragonTargeting.choose(target, position, candidates, s.targetRange(), reevaluate);
        updateState(bounds, s);

        Vector goal = goal(bounds);
        double turnRate = s.turnRate() * (state == State.ATTACK ? 1.5 : 1);
        direction = DragonSteering.heading(position, goal, direction, turnRate);
        position = bounds.clamp(position.clone().add(direction.clone().multiply(s.speed() * speedFactor())));
        if (state == State.PATROL) patrolAngle += s.speed() * speedFactor() / bounds.patrolRadius();

        if (target != null && cooldown == 0 && (state == State.SEEK || state == State.ATTACK)
                && position.distance(target.position()) <= s.attackRange() * 2) {
            cooldown = fireballInterval;
            return target;
        }
        return null;
    }

    private void updateState(DragonBounds bounds, DragonSettings s) {
        switch (state) {
            case PATROL -> {
                if (target != null) enter(State.SEEK);
            }
            case SEEK -> {
                if (target == null) enter(State.RETURN);
                else if (stateTicks >= MIN_SEEK_TICKS && horizontalDistance(position, target.position()) <= s.attackRange()) {
                    enter(State.ATTACK);
                }
            }
            case ATTACK -> {
                if (target == null || stateTicks >= MAX_ATTACK_TICKS
                        || position.distance(target.position()) < HIT_DISTANCE) {
                    enter(State.CLIMB);
                }
            }
            case CLIMB -> {
                if (stateTicks >= CLIMB_TICKS) enter(target != null ? State.SEEK : State.RETURN);
            }
            case RETURN -> {
                if (target != null) {
                    enter(State.SEEK);
                } else {
                    double angle = bounds.angleOf(position);
                    if (position.distance(bounds.patrolPoint(angle)) < PATROL_REACHED) {
                        patrolAngle = angle;
                        enter(State.PATROL);
                    }
                }
            }
        }
    }

    private Vector goal(DragonBounds bounds) {
        return switch (state) {
            case PATROL -> bounds.patrolPoint(patrolAngle + PATROL_LEAD);
            case SEEK -> bounds.clamp(target.position().clone().add(new Vector(0, SEEK_ABOVE, 0)));
            case ATTACK -> bounds.clamp(target.position().clone().add(new Vector(0, ATTACK_ABOVE, 0)));
            case CLIMB -> {
                Vector forward = direction.clone().setY(0);
                if (forward.lengthSquared() < 1.0E-6) forward = new Vector(1, 0, 0);
                yield bounds.clamp(position.clone().add(forward.normalize().multiply(12)).add(new Vector(0, SEEK_ABOVE, 0)));
            }
            case RETURN -> bounds.patrolPoint(bounds.angleOf(position));
        };
    }

    private double speedFactor() {
        return switch (state) {
            case PATROL -> 0.75;
            case SEEK, RETURN -> 1.0;
            case ATTACK -> 1.25;
            case CLIMB -> 0.9;
        };
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
    }

    private static double horizontalDistance(Vector a, Vector b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public Vector position() {
        return position.clone();
    }

    public Vector direction() {
        return direction.clone();
    }

    public State state() {
        return state;
    }

    public Target target() {
        return target;
    }
}
