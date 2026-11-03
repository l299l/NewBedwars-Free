package com.l299l.newbedwars.commands.bedwars;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public abstract class BwCommand extends SubCommand {
    protected final Messages msg = NewBedwars.plugin.getMessages();

    protected void usage(Player player) {
        player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
    }

    protected static String blockXYZ(Location loc) {
        return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }

    protected static Location locationArg(Player player, String[] args, int start) {
        if (args.length == start) return player.getLocation();
        if (args.length != start + 3) return null;
        try {
            return new Location(player.getWorld(),
                    Integer.parseInt(args[start]), Integer.parseInt(args[start + 1]), Integer.parseInt(args[start + 2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
