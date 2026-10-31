package com.l299l.newbedwars.config.data.json.arenas;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;

import java.util.ArrayList;
import java.util.List;

/** Collects problems that stop an arena from being playable. An arena with problems is loaded disabled. */
public final class ArenaValidator {
    private ArenaValidator() {}

    public static List<String> validate(IArena arena) {
        List<String> problems = new ArrayList<>();
        if (arena.getArenaWorld() == null) {
            problems.add("the arena world could not be loaded");
        }
        if (arena.getWaitingSpawn() == null) {
            problems.add("no waiting spawn is set");
        }
        if (arena.getTeams().size() < 2) {
            problems.add("fewer than 2 teams");
        }
        for (Team team : arena.getTeams().values()) {
            if (team.getColor() == null) problems.add("team " + team.getName() + " has no valid color");
            if (team.getTeamSpawn() == null) problems.add("team " + team.getName() + " has no spawn");
            if (team.getTeamBed() == null) problems.add("team " + team.getName() + " has no bed");
        }
        return problems;
    }
}
