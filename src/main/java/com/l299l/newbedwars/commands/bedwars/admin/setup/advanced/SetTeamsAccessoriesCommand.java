package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.commands.bedwars.admin.setup.TabArgsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class SetTeamsAccessoriesCommand extends BwCommand {


    @Override
    public String getName() {
        return "setTeamAccessories";
    }

    @Override
    public String getDescription() {
        return "Set the accessories location for a team.";
    }

    @Override
    public String getSyntax() {
        return "/bw setTeamAccessories <teamName> (<x> <y> <z>)";
    }

    @Override
    public String getExample() {
        return "/bw setTeamAccessories Red";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            usage(player);
            return;
        }
        Team team = arena.getTeams().get(args[1]);
        if (team == null) {
            usage(player);
            return;
        }
        if (args.length == 5) {
            try {
                Location location = new Location(player.getWorld(), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                team.setTeamAccessories(location);
            } catch (Exception e) {
                usage(player);
                return;
            }
        } else {
            team.setTeamAccessories(player.getLocation());
        }
        Location loc = team.getTeamAccessories();
        player.sendMessage(msg.getMsg(player, "TeamAccessoriesSet")
                .replace("/team/", team.getName())
                .replace("/x/", String.valueOf(loc.getBlockX()))
                .replace("/y/", String.valueOf(loc.getBlockY()))
                .replace("/z/", String.valueOf(loc.getBlockZ())));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCordsWithTeam(player, args, 2);
    }
}
