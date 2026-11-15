package com.l299l.newbedwars.arena.setup.auto;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;

import java.util.ArrayList;
import java.util.List;

public record SetupStep(Kind kind, String team) {

    public enum Kind {
        TEAMS, TEAM_BED, TEAM_SPAWN, TEAM_SHOP, TEAM_UPGRADES, TEAM_GENERATOR, TEAM_BASE, TEAM_PROTECTION,
        ARENA_GENERATOR, WAITING_POS1, WAITING_POS2, WAITING_SPAWN, QUICK_VOID
    }

    public String key() {
        return team == null ? kind.name() : kind.name() + ":" + team;
    }

    public static List<SetupStep> missing(IArena arena) {
        List<SetupStep> steps = new ArrayList<>();
        if (arena.getTeams().size() <= 1) steps.add(new SetupStep(Kind.TEAMS, null));
        List<String> names = new ArrayList<>(arena.getTeams().keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        for (String name : names) {
            Team team = arena.getTeams().get(name);
            if (!Boolean.TRUE.equals(team.isBedSet())) steps.add(new SetupStep(Kind.TEAM_BED, name));
            if (!Boolean.TRUE.equals(team.isSpawnSet())) steps.add(new SetupStep(Kind.TEAM_SPAWN, name));
            if (!Boolean.TRUE.equals(team.isShopSet())) steps.add(new SetupStep(Kind.TEAM_SHOP, name));
            if (!Boolean.TRUE.equals(team.isUpgradesSet())) steps.add(new SetupStep(Kind.TEAM_UPGRADES, name));
            if (!Boolean.TRUE.equals(team.isGeneratorSet())) steps.add(new SetupStep(Kind.TEAM_GENERATOR, name));
            if (!team.isBasePosSet()) steps.add(new SetupStep(Kind.TEAM_BASE, name));
            if (!team.isBuildProtAreaPosSet()) steps.add(new SetupStep(Kind.TEAM_PROTECTION, name));
        }
        if (arena.getGenerators().isEmpty()) steps.add(new SetupStep(Kind.ARENA_GENERATOR, null));
        if (arena.getWaitingPos1() == null) steps.add(new SetupStep(Kind.WAITING_POS1, null));
        if (arena.getWaitingPos2() == null) steps.add(new SetupStep(Kind.WAITING_POS2, null));
        if (arena.getWaitingSpawn() == null) steps.add(new SetupStep(Kind.WAITING_SPAWN, null));
        if (arena.getQuickVoidY() == null) steps.add(new SetupStep(Kind.QUICK_VOID, null));
        return steps;
    }
}
