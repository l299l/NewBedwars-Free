package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class SetWaitingSpawnCommand extends SubCommand {
    private final Messages msg;

    public SetWaitingSpawnCommand() {
        this.msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "setWaitingSpawn";
    }

    @Override
    public String getDescription() {
        return "Command to set waitingSpawn before start.";
    }

    @Override
    public String getSyntax() {
        return "/bw waitingSpawn (<x> <y> <z>)";
    }

    @Override
    public String getExample() {
        return "/bw waitingSPawn";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if(args.length == 4) {
            try {
                Location loc = new Location(player.getWorld(), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                arena.setWaitingSpawn(loc);
                player.sendMessage(msg.getMsg(player, "WaitingSpawnSet") + arena.getWaitingSpawn().getBlockX() + " " + arena.getWaitingSpawn().getBlockY() + " " + arena.getWaitingSpawn().getBlockZ());
            }catch (Exception e) {
                player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            }
        }else {
            arena.setWaitingSpawn(player.getLocation());
            player.sendMessage(msg.getMsg(player, "WaitingSpawnSet") + arena.getWaitingSpawn().getBlockX() + " " + arena.getWaitingSpawn().getBlockY() + " " + arena.getWaitingSpawn().getBlockZ());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabCords(player, args, 2);
    }
}
