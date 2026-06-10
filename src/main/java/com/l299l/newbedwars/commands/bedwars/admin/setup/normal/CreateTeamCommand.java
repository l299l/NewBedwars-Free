package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateTeamCommand extends SubCommand {
    private final Messages msg;

    public CreateTeamCommand() {
        msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "createTeam";
    }

    @Override
    public String getDescription() {
        return "This command for creating bedwars teams!";
    }

    @Override
    public String getSyntax() {
        return "/bw createTeam <color> <name>";
    }

    @Override
    public String getExample() {
        return "/bw createTeam RED Red";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if(args.length >= 2) {
            try {
                ChatColor color = ChatColor.valueOf(args[1].toUpperCase());
                String name;
                if(args.length == 3) {
                    name = args[2];
                }else {
                    name = args[1];
                }
                arena.createTeam(name, color);
                player.sendMessage(msg.getMsg(player, "createTeamSuccess") + name);
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
            return new ArrayList<>(Arrays.asList("RED", "BLUE", "GREEN", "YELLOW", "PINK", "GRAY", "LIGHT_PURPLE", "AQUA", "LIME", "ORANGE", "PURPLE", "WHITE", "BLACK"));
        }
        return null;
    }
}
