package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class SetWaitingPos1Command extends SubCommand {
    private final Messages msg;

    public SetWaitingPos1Command() {
        this.msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "setWaitingPos1";
    }

    @Override
    public String getDescription() {
        return "Command to set first position of waiting region.";
    }

    @Override
    public String getSyntax() {
        return "/bw waitingPos1 (<x> <y> <z>)";
    }

    @Override
    public String getExample() {
        return "/bw waitingPos1";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if(args.length == 4) {
            try {
                Location loc = new Location(player.getWorld(), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                arena.setWaitingPos1(loc);
                player.sendMessage(msg.getMsg(player, "WaitingPos1Set") + arena.getWaitingPos1().getBlockX() + " " + arena.getWaitingPos1().getBlockY() + " " + arena.getWaitingPos1().getBlockZ());
            }catch (Exception e) {
                player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            }
        }else {
            arena.setWaitingPos1(player.getLocation());
            player.sendMessage(msg.getMsg(player, "WaitingPos1Set") + arena.getWaitingPos1().getBlockX() + " " + arena.getWaitingPos1().getBlockY() + " " + arena.getWaitingPos1().getBlockZ());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCords(player, args, 2);
    }
}
