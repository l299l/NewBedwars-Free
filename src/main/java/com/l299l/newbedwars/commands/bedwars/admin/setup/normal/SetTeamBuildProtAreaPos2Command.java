package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class SetTeamBuildProtAreaPos2Command extends SubCommand {
    private final Messages msg;

    public SetTeamBuildProtAreaPos2Command() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "setTeamBuildProtAreaPos2"; }
    @Override public String getDescription() { return "Set corner 2 of the team's block-protection zone (no placement inside)."; }
    @Override public String getSyntax() { return "/bw setTeamBuildProtAreaPos2 <teamName>"; }
    @Override public String getExample() { return "/bw setTeamBuildProtAreaPos2 Red"; }

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
        team.setTeamBuildProtAreaPos2(player.getLocation());
        player.sendMessage(msg.getMsg(player, "TeamBuildProtAreaPos2Set")
                + team.getTeamBuildProtAreaPos2().getBlockX() + " "
                + team.getTeamBuildProtAreaPos2().getBlockY() + " "
                + team.getTeamBuildProtAreaPos2().getBlockZ());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCordsWithTeam(player, args, 2);
    }
}
