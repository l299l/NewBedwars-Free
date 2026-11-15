package com.l299l.newbedwars.arena.setup.auto;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AreaTracker {
    private final Map<String, Box> base = new HashMap<>();
    private final Map<String, Box> protection = new HashMap<>();

    public void refresh(IArena arena) {
        for (Team team : arena.getTeams().values()) {
            List<Pos> points = points(team);
            if (points.isEmpty()) continue;
            Box newBase = AreaGuesser.base(points, new AreaGuesser.Padding(
                    Properties.AutoSetupBasePadding, Properties.AutoSetupBaseBelow, Properties.AutoSetupBaseAbove));
            if (isAuto(base.get(team.getName()), team.getTeamBasePos1(), team.getTeamBasePos2())) {
                team.setTeamBasePos1(corner(arena, newBase.min()));
                team.setTeamBasePos2(corner(arena, newBase.max()));
                base.put(team.getName(), newBase);
            }
            Box newProtection = AreaGuesser.buildProtection(pos(team.getTeamSpawn()),
                    team.getGenerator() == null ? null : pos(team.getGenerator().getLocation()), points);
            if (isAuto(protection.get(team.getName()), team.getTeamBuildProtAreaPos1(), team.getTeamBuildProtAreaPos2())) {
                team.setTeamBuildProtAreaPos1(corner(arena, newProtection.min()));
                team.setTeamBuildProtAreaPos2(corner(arena, newProtection.max()));
                protection.put(team.getName(), newProtection);
            }
        }
    }

    static boolean isAuto(Box guessed, Location pos1, Location pos2) {
        if (pos1 == null && pos2 == null) return true;
        if (guessed == null || pos1 == null || pos2 == null) return false;
        return same(guessed.min(), pos1) && same(guessed.max(), pos2);
    }

    private static boolean same(Pos pos, Location loc) {
        return pos.x() == loc.getBlockX() && pos.y() == loc.getBlockY() && pos.z() == loc.getBlockZ();
    }

    private static List<Pos> points(Team team) {
        List<Pos> points = new ArrayList<>();
        if (team.getTeamBed() != null) points.add(pos(team.getTeamBed().getLocation()));
        if (team.getTeamSpawn() != null) points.add(pos(team.getTeamSpawn()));
        if (team.getTeamShop() != null) points.add(pos(team.getTeamShop().getLocation()));
        if (team.getTeamUpgrades() != null) points.add(pos(team.getTeamUpgrades().getLocation()));
        if (team.getGenerator() != null) points.add(pos(team.getGenerator().getLocation()));
        return points;
    }

    private static Pos pos(Location location) {
        return location == null ? null : new Pos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static Location corner(IArena arena, Pos pos) {
        return new Location(arena.getArenaWorld(), pos.x(), pos.y(), pos.z());
    }
}
