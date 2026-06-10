package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class SetMinPlayersCommand extends SubCommand {
    private final Messages msg;

    public SetMinPlayersCommand() {
        msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "setMinPlayers";
    }

    @Override
    public String getDescription() {
        return "Simple command to set minimum players in arena!";
    }

    @Override
    public String getSyntax() {
        return "/bw setMinPlayers <value>";
    }

    @Override
    public String getExample() {
        return "/bw setMinPlayers 4";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if(args.length == 2) {
            try {
                arena.setMinPlayers(Integer.parseInt(args[1]));
                player.sendMessage(msg.getMsg(player, "setMinPlayersSuccess") + arena.getMinPlayers());
            }catch (Exception e) {
                player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            }
        }else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getCommandNums(args);
    }
}
