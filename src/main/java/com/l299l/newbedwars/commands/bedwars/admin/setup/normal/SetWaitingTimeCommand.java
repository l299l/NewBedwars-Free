package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class SetWaitingTimeCommand extends BwCommand {

    @Override
    public String getName() {
        return "setWaitingTime";
    }

    @Override
    public String getDescription() {
        return "Command to set time before start.";
    }

    @Override
    public String getSyntax() {
        return "/bw waitingTime <time>";
    }

    @Override
    public String getExample() {
        return "/bw waitingTime 30";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if(args.length == 2) {
            try {
                arena.setWaitingTime(Integer.parseInt(args[1]));
                player.sendMessage(msg.getMsg(player, "setWaitingTimeSuccess") + arena.getWaitingTime());
            } catch (Exception e) {
                usage(player);
            }
        }else {
            usage(player);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
