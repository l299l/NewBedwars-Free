package com.l299l.newbedwars.npc.fallback;

import com.l299l.newbedwars.npc.JoinNpcAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/** A plain entity used as a join NPC when Citizens is not installed. Saved in {@code data/npcs.json}. */
public final class FallbackNpc {
    public int id;
    public String entityType;
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public String action;
    public String target;
    /** The spawned entity; not saved. */
    public transient UUID entity;

    public FallbackNpc() {}

    public FallbackNpc(int id, String entityType, Location loc, JoinNpcAction action) {
        this.id = id;
        this.entityType = entityType;
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
        this.action = action.getType().name();
        this.target = action.getTarget();
    }

    /** The stored action, or null when the file holds an invalid one. */
    public JoinNpcAction joinAction() {
        return JoinNpcAction.tryParse(action, target);
    }

    /** The stored location, or null while its world is not loaded. */
    public Location location() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
