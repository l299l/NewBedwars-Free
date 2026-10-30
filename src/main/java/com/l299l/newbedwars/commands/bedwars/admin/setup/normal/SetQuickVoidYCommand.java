package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class SetQuickVoidYCommand extends BwCommand {


    @Override public String getName() { return "setQuickVoidY"; }
    @Override public String getDescription() { return "Set the Y level at which players instantly die (quick void)."; }
    @Override public String getSyntax() { return "/bw setQuickVoidY (<y>)"; }
    @Override public String getExample() { return "/bw setQuickVoidY -40"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        int y;
        if (args.length >= 2) {
            try {
                y = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                usage(player);
                return;
            }
        } else {
            y = player.getLocation().getBlockY();
        }
        arena.setQuickVoidY(y);
        player.sendMessage(msg.getMsg(player, "QuickVoidYSet") + y);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.of(String.valueOf(player.getLocation().getBlockY()));
        return null;
    }
}
