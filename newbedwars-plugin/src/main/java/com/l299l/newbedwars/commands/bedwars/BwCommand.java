package com.l299l.newbedwars.commands.bedwars;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Base for the plugin's own {@code /bw} subcommands: holds {@link Messages} and common replies. */
public abstract class BwCommand extends SubCommand {
    protected final Messages msg = NewBedwars.plugin.getMessages();

    /** Sends "Correct usage: " followed by {@link #getSyntax()}. */
    protected void usage(Player player) {
        player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
    }

    /** {@code "x y z"} of the block coordinates. */
    protected static String blockXYZ(Location loc) {
        return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }

    /**
     * The location given by {@code args[start..start+2]}, or the player's location when no coordinates were given.
     * Returns {@code null} for any other argument count or for non-numeric coordinates.
     */
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
