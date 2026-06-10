package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class SetTeamBasePos2Command extends SubCommand {
    private final Messages msg;

    public SetTeamBasePos2Command() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "setTeamBasePos2"; }
    @Override public String getDescription() { return "Set corner 2 of the team's whole base region (used for traps and heal pool)."; }
    @Override public String getSyntax() { return "/bw setTeamBasePos2 <teamName>"; }
    @Override public String getExample() { return "/bw setTeamBasePos2 Red"; }

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
        team.setTeamBasePos2(player.getLocation());
        player.sendMessage(msg.getMsg(player, "TeamBasePos2Set")
                + team.getTeamBasePos2().getBlockX() + " "
                + team.getTeamBasePos2().getBlockY() + " "
                + team.getTeamBasePos2().getBlockZ());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCordsWithTeam(player, args, 2);
    }
}
