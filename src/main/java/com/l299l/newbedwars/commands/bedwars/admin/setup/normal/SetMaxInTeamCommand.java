package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetMaxInTeamCommand extends SubCommand {
    private final Messages msg;

    public SetMaxInTeamCommand() {
        msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "setMaxInTeam";
    }

    @Override
    public String getDescription() {
        return "Simple command to set max players in team!";
    }

    @Override
    public String getSyntax() {
        return "/bw setMaxInTeam <number>";
    }

    @Override
    public String getExample() {
        return "/bw setMaxInTeam 3";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if(args.length == 2) {
            try {
                arena.setMaxInTeam(Integer.parseInt(args[1]));
                player.sendMessage(msg.getMsg(player, "setMaxInTeamSuccess") + arena.getMaxInTeam());
            }catch (Exception e) {
                player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            }
        }else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }

    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if(args.length == 2) {
            List<String> arguments = new ArrayList<>();
            for (int i = 1; i < 5; i++) {
                arguments.add(String.valueOf(i));
            }
            return arguments;
        }else {
            return null;
        }
    }
}
