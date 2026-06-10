package com.l299l.newbedwars.arena.team;


import org.bukkit.Location;

public class TeamBed {
    private final Location location;
    private final Team team;
    private boolean isAlive = true;

    public TeamBed(Location location, Team team) {
        this.location = location;
        this.team = team;
    }

    public void destroy() {
        isAlive = false;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public Location getLocation() {
        return location;
    }

    public Team getTeam() {
        return team;
    }

    public void respawn() {
        isAlive = true;
    }

}
