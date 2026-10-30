package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class RejoinCommand extends BwCommand {


    @Override
    public String getName() {
        return "rejoin";
    }

    @Override
    public String getDescription() {
        return "Command to rejoin arena";
    }

    @Override
    public String getSyntax() {
        return "/bw rejoin";
    }

    @Override
    public String getExample() {
        return "/bw rejoin";
    }

    @Override
    public void perform(Player p, String[] args, IArena arena) {
        if (args.length > 1) {
            usage(p);
            return;
        }
        IArena physicalArena = ArenaRegistry.byWorld(p.getWorld());
        if (physicalArena != null && physicalArena.isPlayerInArena(p)
                && !physicalArena.getSpectators().contains(p)) {
            msg.send(p, "AlreadyInArena");
            return;
        }
        // Find a pending rejoin slot (player kept in players map during disconnect window)
        IArena rejoinArena = findRejoinArena(p);
        if (rejoinArena == null) {
            msg.send(p, "NoArenaToRejoin");
            return;
        }
        if (rejoinArena.rejoin(p)) {
            msg.send(p, "Rejoined");
        } else {
            msg.send(p, "RejoinFailed");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }

    private IArena findRejoinArena(Player p) {
        for (IArena arena : ArenaRegistry.all()) {
            if (arena.isPlayerInArena(p)) {
                return arena;
            }
        }
        return null;
    }
}
