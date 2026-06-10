package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.DecoUtils;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class SetTeamBedCommand extends SubCommand {
    private final Messages msg;

    public SetTeamBedCommand() {
        msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "setTeamBed";
    }

    @Override
    public String getDescription() {
        return "Command to set Teams Bed";
    }

    @Override
    public String getSyntax() {
        return "/bw setTeamBed <teamName> (<x> <y> <z>)";
    }

    @Override
    public String getExample() {
        return "/bw setTeamBed";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if(args.length < 2) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        Team team = arena.getTeams().get(args[1]);
        if(team != null) {
            if(args.length == 5) {
                try {
                    Location location = new Location(player.getWorld(), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                    if (!isBedPlaced(location)) {
                        player.sendMessage(msg.getMsg(player, "placeBedFirst"));
                        return;
                    }
                    if (team.isBedSet()) DecoUtils.removeArmorStandAt(team.getTeamBed().getLocation());
                    arena.getTeams().get(args[1]).setTeamBed(location);
                }catch (Exception e) {
                    player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                }
            }else {
                if (!isBedPlaced(player.getLocation())) {
                    player.sendMessage(msg.getMsg(player, "placeBedFirst"));
                    return;
                }
                if (team.isBedSet()) DecoUtils.removeArmorStandAt(team.getTeamBed().getLocation());
                arena.getTeams().get(args[1]).setTeamBed(player.getLocation());
            }
            player.sendMessage(msg.getMsg(player, "setTeamBedSuccess") + team.getTeamBed().getLocation().getBlockX() + " " + team.getTeamBed().getLocation().getBlockY() + " " + team.getTeamBed().getLocation().getBlockZ());
            DecoUtils.summonSetupArmorStand(team.getTeamBed().getLocation(), team.getColor() + "Bed");
        }else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCordsWithTeam(player, args, 2);
    }

    private boolean isBedPlaced(Location location) {
        if(location.getBlock().getType().name().contains("BED")) {
            return true;
        }else return location.getBlock().getRelative(0, 1, 0).getType().name().contains("BED");
    }
}
