package com.l299l.newbedwars.npc.fallback;

import com.l299l.newbedwars.npc.JoinNpcAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

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

    public JoinNpcAction joinAction() {
        return JoinNpcAction.tryParse(action, target);
    }

    public Location location() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
