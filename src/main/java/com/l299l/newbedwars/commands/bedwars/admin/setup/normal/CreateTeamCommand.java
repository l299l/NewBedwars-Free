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
                player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            }
        }else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if(args.length == 2) {
            return new ArrayList<>(Arrays.asList("WHITE", "LIGHT_GRAY", "GRAY", "BLACK", "RED", "ORANGE", "YELLOW", "LIME",
                    "GREEN", "CYAN", "LIGHT_BLUE", "BLUE", "PURPLE"));
        }
        return null;
    }

    private ChatColor parseColor(String color) {
        ChatColor c;
        switch (color) {
            case "PINK" -> c = ChatColor.LIGHT_PURPLE;
            case "ORANGE" -> c = ChatColor.GOLD;
            case "LIME" -> c = ChatColor.GREEN;
            case "LIGHT_GRAY" -> c = ChatColor.GRAY;
            case "GRAY" -> c = ChatColor.DARK_GRAY;
            case "CYAN" -> c = ChatColor.DARK_AQUA;
            case "LIGHT_BLUE" -> c = ChatColor.AQUA;
            case "BLUE" -> c = ChatColor.DARK_BLUE;
            case "PURPLE" -> c = ChatColor.DARK_PURPLE;
            default -> c = ChatColor.valueOf(color);
        }
        return c;
    }
}
