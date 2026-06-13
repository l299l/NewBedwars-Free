package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReloadCommand extends SubCommand {
    private final Messages msg;

    public ReloadCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload the plugin configuration and data.";
    }

    @Override
    public String getSyntax() {
        return "/bw reload";
    }

    @Override
    public String getExample() {
        return "/bw reload";
    }

    @Override
    public void perform(Player p, String[] args, IArena arena) {
        if (!p.hasPermission("newbedwars.bw.admin") && !p.isOp()) {
            msg.send(p, "NoPermissions");
            return;
        }

        for (IArena a : new ArrayList<>(Arena.arenaByName.values())) {
            for (UUID playerId : new ArrayList<>(a.getPlayers())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    a.leave(player);
                }
            }
        }

        NewBedwars.plugin.getDataManager().save();
        NewBedwars.plugin.reloadAll();
        NewBedwars.plugin.getDataManager().loadArenas();
        msg.send(p, "ReloadSuccess");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
