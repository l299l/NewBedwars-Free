package com.l299l.newbedwars.arena.dragon;

import com.l299l.newbedwars.arena.dragon.DragonTargeting.Candidates;
import com.l299l.newbedwars.arena.dragon.DragonTargeting.Target;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

final class TeamDragon {
    private static final double FIREBALL_DROP = 2;
    private static final double FIREBALL_FORWARD = 2;

    private final Team team;
    private final EnderDragon entity;
    private final DragonBrain brain;
    private final String ownerKey;

    TeamDragon(Team team, EnderDragon entity, DragonBrain brain, String ownerKey) {
        this.team = team;
        this.entity = entity;
        this.brain = brain;
        this.ownerKey = ownerKey;
    }

    void tick(World world, DragonBounds bounds, Candidates candidates, DragonSettings settings) {
        Target shot = brain.tick(bounds, candidates, settings);
        Vector p = brain.position();
        Vector d = brain.direction();
        entity.teleport(new Location(world, p.getX(), p.getY(), p.getZ(), DragonSteering.yaw(d), 0f));
        entity.setVelocity(new Vector());
        holdStill();
        if (shot != null) fire(world, shot, settings);
    }

    private void holdStill() {
        entity.setPhase(EnderDragon.Phase.CIRCLING);
        entity.setPhase(EnderDragon.Phase.HOVER);
    }

    private void fire(World world, Target shot, DragonSettings settings) {
        Vector from = brain.position().add(new Vector(0, -FIREBALL_DROP, 0));
        Vector forward = shot.position().clone().subtract(from).setY(0);
        if (forward.lengthSquared() > 1.0E-6) from.add(forward.normalize().multiply(FIREBALL_FORWARD));
        Vector aim = DragonSteering.lead(from, shot.position(), shot.velocity(), settings.fireballSpeed());
        Vector direction = aim.subtract(from);
        if (direction.lengthSquared() < 1.0E-6) return;
        direction.normalize();
        DragonFireball fireball = (DragonFireball) world.spawnEntity(
                new Location(world, from.getX(), from.getY(), from.getZ()), EntityType.DRAGON_FIREBALL);
        fireball.setShooter(entity);
        fireball.setDirection(direction);
        fireball.setVelocity(direction.clone().multiply(settings.fireballSpeed()));
        DragonController.tag(fireball, ownerKey);
    }

    void remove() {
        if (entity.isValid()) entity.remove();
    }

    boolean isValid() {
        return entity.isValid();
    }

    Team team() {
        return team;
    }

    Vector position() {
        return brain.position();
    }
}
