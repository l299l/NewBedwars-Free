package com.l299l.newbedwars.commands.bedwars.main;

import com.google.common.collect.Lists;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.List;

public class RejoinCommand extends SubCommand {
    private final Messages msg;

    public RejoinCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

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
            p.sendMessage(msg.getMsg(p, "CorrectUsage") + getSyntax());
            return;
        }
        // If the player is physically inside an arena and actively playing, block rejoin
        IArena physicalArena = Arena.arenaByWorld.get(p.getWorld());
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
        for (IArena arena : Arena.arenaByName.values()) {
            if (arena.isPlayerInArena(p)) {
                return arena;
            }
        }
        return null;
    }
}
