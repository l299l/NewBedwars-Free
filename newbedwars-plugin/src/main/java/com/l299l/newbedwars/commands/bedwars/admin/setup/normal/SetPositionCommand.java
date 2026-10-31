package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.commands.bedwars.admin.setup.TabArgsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** {@code /bw set<Point> [team] [x y z]}: stores one arena or team location (the player's own when no coordinates are given). */
public class SetPositionCommand extends BwCommand {

    public enum Point {
        WAITING_POS1("setWaitingPos1", "Set corner 1 of the waiting lobby region.", "WaitingPos1Set",
                IArena::setWaitingPos1, IArena::getWaitingPos1),
        WAITING_POS2("setWaitingPos2", "Set corner 2 of the waiting lobby region.", "WaitingPos2Set",
                IArena::setWaitingPos2, IArena::getWaitingPos2),
        WAITING_SPAWN("setWaitingSpawn", "Set the spawn of the waiting lobby.", "WaitingSpawnSet",
                IArena::setWaitingSpawn, IArena::getWaitingSpawn),
        TEAM_SPAWN("setTeamSpawn", "Set the team's spawn.", "setTeamSpawnSuccess",
                Team::setTeamSpawn, Team::getTeamSpawn, "Spawn"),
        TEAM_BASE_POS1("setTeamBasePos1", "Set corner 1 of the team's whole base region (used for traps and heal pool).", "TeamBasePos1Set",
                Team::setTeamBasePos1, Team::getTeamBasePos1, null),
        TEAM_BASE_POS2("setTeamBasePos2", "Set corner 2 of the team's whole base region (used for traps and heal pool).", "TeamBasePos2Set",
                Team::setTeamBasePos2, Team::getTeamBasePos2, null),
        TEAM_BUILD_PROT_POS1("setTeamBuildProtAreaPos1", "Set corner 1 of the team's block-protection zone (no placement inside).", "TeamBuildProtAreaPos1Set",
                Team::setTeamBuildProtAreaPos1, Team::getTeamBuildProtAreaPos1, null),
        TEAM_BUILD_PROT_POS2("setTeamBuildProtAreaPos2", "Set corner 2 of the team's block-protection zone (no placement inside).", "TeamBuildProtAreaPos2Set",
                Team::setTeamBuildProtAreaPos2, Team::getTeamBuildProtAreaPos2, null);

        final String command;
        final String description;
        final String messageKey;
        final BiConsumer<IArena, Location> arenaSetter;
        final Function<IArena, Location> arenaGetter;
        final BiConsumer<Team, Location> teamSetter;
        final Function<Team, Location> teamGetter;
        /** Label of the setup armor stand placed at the point, or {@code null} for none. */
        final String marker;

        Point(String command, String description, String messageKey,
              BiConsumer<IArena, Location> setter, Function<IArena, Location> getter) {
            this(command, description, messageKey, setter, getter, null, null, null);
        }

        Point(String command, String description, String messageKey,
              BiConsumer<Team, Location> setter, Function<Team, Location> getter, String marker) {
            this(command, description, messageKey, null, null, setter, getter, marker);
        }

        Point(String command, String description, String messageKey,
              BiConsumer<IArena, Location> arenaSetter, Function<IArena, Location> arenaGetter,
              BiConsumer<Team, Location> teamSetter, Function<Team, Location> teamGetter, String marker) {
            this.command = command;
            this.description = description;
            this.messageKey = messageKey;
            this.arenaSetter = arenaSetter;
            this.arenaGetter = arenaGetter;
            this.teamSetter = teamSetter;
            this.teamGetter = teamGetter;
            this.marker = marker;
        }

        boolean isTeamPoint() {
            return teamSetter != null;
        }
    }

    private final Point point;

    public SetPositionCommand(Point point) {
        this.point = point;
    }

    @Override public String getName() { return point.command; }
    @Override public String getDescription() { return point.description; }

    @Override
    public String getSyntax() {
        return "/bw " + point.command + (point.isTeamPoint() ? " <teamName>" : "") + " (<x> <y> <z>)";
    }

    @Override
    public String getExample() {
        return "/bw " + point.command + (point.isTeamPoint() ? " Red" : "");
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (!point.isTeamPoint()) {
            Location loc = locationArg(player, args, 1);
            if (loc == null) {
                usage(player);
                return;
            }
            point.arenaSetter.accept(arena, loc);
            player.sendMessage(msg.getMsg(player, point.messageKey) + blockXYZ(point.arenaGetter.apply(arena)));
            return;
        }
        Team team = args.length >= 2 ? arena.getTeams().get(args[1]) : null;
        Location loc = team == null ? null : locationArg(player, args, 2);
        if (loc == null) {
            usage(player);
            return;
        }
        Location old = point.teamGetter.apply(team);
        if (point.marker != null && old != null) DecoUtils.removeArmorStandAt(old);
        point.teamSetter.accept(team, loc);
        Location set = point.teamGetter.apply(team);
        player.sendMessage(msg.getMsg(player, point.messageKey) + blockXYZ(set));
        if (point.marker != null) DecoUtils.summonSetupArmorStand(set, team.getColor() + point.marker);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return point.isTeamPoint()
                ? TabArgsUtils.getTabCordsWithTeam(player, args, 2)
                : TabArgsUtils.getTabCords(player, args, 2);
    }
}
