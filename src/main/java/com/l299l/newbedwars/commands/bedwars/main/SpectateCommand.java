package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpectateCommand extends SubCommand {
    private final Messages msg;

    public SpectateCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "spectate"; }
    @Override public String getDescription() { return "Join a running arena as spectator."; }
    @Override public String getSyntax() { return "/bw spectate <arena>"; }
    @Override public String getExample() { return "/bw spectate myArena"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.AQUA + "Arenas available to spectate:");
            boolean any = false;
            for (IArena a : Arena.arenaByName.values()) {
                if ((a.status() == GameStatus.playing || a.status() == GameStatus.ending)
                        && a.getGamerules() != null && a.getGamerules().AllowSpectators) {
                    player.sendMessage(ChatColor.YELLOW + "  - " + a.getArenaName());
                    any = true;
                }
            }
            if (!any) player.sendMessage(ChatColor.RED + "  No arenas available to spectate right now.");
            return;
        }
        String arenaName = args[1];
        IArena target = Arena.arenaByName.get(arenaName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found.");
            return;
        }
        if (!target.joinAsSpectator(player)) {
            if (target.status() != GameStatus.playing && target.status() != GameStatus.ending) {
                player.sendMessage(ChatColor.RED + "That arena is not currently running.");
            } else if (target.getGamerules() == null || !target.getGamerules().AllowSpectators) {
                player.sendMessage(ChatColor.RED + "Spectating is disabled for that arena.");
            } else {
                player.sendMessage(ChatColor.RED + "Could not join as spectator.");
            }
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (IArena a : Arena.arenaByName.values()) {
                if ((a.status() == GameStatus.playing || a.status() == GameStatus.ending)
                        && a.getGamerules() != null && a.getGamerules().AllowSpectators) {
                    names.add(a.getArenaName());
                }
            }
            return names;
        }
        return null;
    }
}
