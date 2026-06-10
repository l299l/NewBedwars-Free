package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class SetTeamBuildProtAreaPos1Command extends SubCommand {
    private final Messages msg;

    public SetTeamBuildProtAreaPos1Command() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "setTeamBuildProtAreaPos1"; }
    @Override public String getDescription() { return "Set corner 1 of the team's block-protection zone (no placement inside)."; }
    @Override public String getSyntax() { return "/bw setTeamBuildProtAreaPos1 <teamName>"; }
    @Override public String getExample() { return "/bw setTeamBuildProtAreaPos1 Red"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        Team team = arena.getTeams().get(args[1]);
        if (team == null) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        team.setTeamBuildProtAreaPos1(player.getLocation());
        player.sendMessage(msg.getMsg(player, "TeamBuildProtAreaPos1Set")
                + team.getTeamBuildProtAreaPos1().getBlockX() + " "
                + team.getTeamBuildProtAreaPos1().getBlockY() + " "
                + team.getTeamBuildProtAreaPos1().getBlockZ());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCordsWithTeam(player, args, 2);
    }
}
