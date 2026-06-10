package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class SetTeamBasePos1Command extends SubCommand {
    private final Messages msg;

    public SetTeamBasePos1Command() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "setTeamBasePos1"; }
    @Override public String getDescription() { return "Set corner 1 of the team's whole base region (used for traps and heal pool)."; }
    @Override public String getSyntax() { return "/bw setTeamBasePos1 <teamName>"; }
    @Override public String getExample() { return "/bw setTeamBasePos1 Red"; }

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
        team.setTeamBasePos1(player.getLocation());
        player.sendMessage(msg.getMsg(player, "TeamBasePos1Set")
                + team.getTeamBasePos1().getBlockX() + " "
                + team.getTeamBasePos1().getBlockY() + " "
                + team.getTeamBasePos1().getBlockZ());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCordsWithTeam(player, args, 2);
    }
}
