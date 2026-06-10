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

public class SetTeamsSpawnCommand extends SubCommand {
    private final Messages msg;

    public SetTeamsSpawnCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "setTeamSpawn";
    }

    @Override
    public String getDescription() {
        return "Command to set teams spawns.";
    }

    @Override
    public String getSyntax() {
        return "/bw setTeamSpawn <teamName> (<x> <y> <z>)";
    }

    @Override
    public String getExample() {
        return "/bw setTeamShop Red";
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
                    if (team.isSpawnSet()) DecoUtils.removeArmorStandAt(team.getTeamSpawn());
                    arena.getTeams().get(args[1]).setTeamSpawn(new Location(player.getWorld(), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4])));
                    player.sendMessage(msg.getMsg(player, "setTeamSpawnSuccess") + team.getTeamSpawn().getBlockX() + " " + team.getTeamSpawn().getBlockY() + " " + team.getTeamSpawn().getBlockZ());
                    DecoUtils.summonSetupArmorStand(team.getTeamSpawn(), team.getColor() + "Spawn");
                }catch (Exception e) {
                    player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                }
            }else {
                if (team.isSpawnSet()) DecoUtils.removeArmorStandAt(team.getTeamSpawn());
                arena.getTeams().get(args[1]).setTeamSpawn(player.getLocation());
                player.sendMessage(msg.getMsg(player, "setTeamSpawnSuccess") + team.getTeamSpawn().getBlockX() + " " + team.getTeamSpawn().getBlockY() + " " + team.getTeamSpawn().getBlockZ());
                DecoUtils.summonSetupArmorStand(team.getTeamSpawn(), team.getColor() + "Spawn");
            }
        }else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCordsWithTeam(player, args, 2);
    }
}
