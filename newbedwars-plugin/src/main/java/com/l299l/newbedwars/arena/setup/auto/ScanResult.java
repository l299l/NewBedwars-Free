package com.l299l.newbedwars.arena.setup.auto;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class ScanResult {

    public static final class TeamPlan {
        public final ChatColor color;
        public final String name;
        public Pos bed;
        public Pos spawn;
        public Pos shop;
        public String shopEntity;
        public Pos upgrades;
        public String upgradesEntity;
        public Pos generator;

        TeamPlan(ChatColor color, String name, Pos bed) {
            this.color = color;
            this.name = name;
            this.bed = bed;
        }
    }

    public record Warning(String key, String detail) {}

    public final List<TeamPlan> teams = new ArrayList<>();
    public final List<Pos> diamondGenerators = new ArrayList<>();
    public final List<Pos> emeraldGenerators = new ArrayList<>();
    public final List<SignMark> usedSigns = new ArrayList<>();
    public final List<Warning> warnings = new ArrayList<>();
    public Integer quickVoidY;

    public TeamPlan team(ChatColor color) {
        for (TeamPlan team : teams) {
            if (team.color == color) return team;
        }
        return null;
    }
}
