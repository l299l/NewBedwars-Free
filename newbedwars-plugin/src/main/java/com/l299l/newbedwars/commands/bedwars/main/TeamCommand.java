package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.arena.team.TeamSelectorManager;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeamCommand extends BwCommand {

    @Override public String getName() { return "team"; }
    @Override public String getDescription() { return "Pick your team before the game starts."; }
    @Override public String getSyntax() { return "/bw team [team|random]"; }
    @Override public String getExample() { return "/bw team red"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        TeamSelectorManager manager = TeamSelectorManager.get();
        if (arena == null || manager == null) {
            msg.send(player, "TeamSelectorNotNow");
            return;
        }
        if (args.length < 2) {
            manager.open(player, arena);
            return;
        }
        if (args[1].equalsIgnoreCase("random")) {
            manager.pick(player, arena, null);
            return;
        }
        Team found = null;
        for (Team team : arena.getTeams().values()) {
            String display = ChatColor.stripColor(team.getDisplayName());
            if (team.getName().equalsIgnoreCase(args[1]) || (display != null && display.equalsIgnoreCase(args[1]))) {
                found = team;
                break;
            }
        }
        if (found == null) {
            msg.send(player, "TeamSelectorUnknownTeam", java.util.Map.of("/team/", args[1]));
            return;
        }
        manager.pick(player, arena, found.getName());
    }

    @Override
    public boolean isAvailable(Player player) {
        TeamSelectorManager manager = TeamSelectorManager.get();
        return manager != null && manager.canPickNow(player, ArenaRegistry.byWorld(player.getWorld()));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length != 2 || !isAvailable(player)) return null;
        List<String> names = new ArrayList<>();
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        names.addAll(arena.getTeams().keySet());
        names.add("random");
        return names;
    }
}
