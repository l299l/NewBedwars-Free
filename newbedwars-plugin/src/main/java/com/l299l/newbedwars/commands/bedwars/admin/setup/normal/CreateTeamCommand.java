package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.utils.TeamColor;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateTeamCommand extends BwCommand {

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
                ChatColor color = parseColor(args[1].toUpperCase());
                String name;
                if(args.length == 3) {
                    name = args[2];
                }else {
                    name = args[1];
                }
                arena.createTeam(name, color);
                player.sendMessage(msg.getMsg(player, "createTeamSuccess") + name);
            }catch (Exception e) {
                usage(player);
            }
        }else {
            usage(player);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if(args.length == 2) {
            return new ArrayList<>(Arrays.asList("WHITE", "LIGHT_GRAY", "GRAY", "BLACK", "RED", "ORANGE", "YELLOW", "LIME",
                    "GREEN", "CYAN", "LIGHT_BLUE", "BLUE", "PURPLE", "PINK"));
        }
        return null;
    }

    private ChatColor parseColor(String color) {
        return TeamColor.parse(color);
    }
}
