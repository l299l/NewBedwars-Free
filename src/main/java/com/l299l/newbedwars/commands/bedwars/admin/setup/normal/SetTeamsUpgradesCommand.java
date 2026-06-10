package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.DecoUtils;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;

public class SetTeamsUpgradesCommand extends SubCommand {
    private final Messages msg;

    public SetTeamsUpgradesCommand() {
        msg = NewBedwars.plugin.getMessages();
    }
    @Override
    public String getName() {
        return "setTeamUpgrades";
    }

    @Override
    public String getDescription() {
        return "Command to set team upgrades.";
    }

    @Override
    public String getSyntax() {
        return "/bw <teamName> <entityType> (<x> <y> <z>)";
    }

    @Override
    public String getExample() {
        return "/bw setTeamShop Red VILLAGER";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if(args.length >= 3) {
            try {
                Location loc = player.getLocation();
                if (args.length == 6) {
                    loc = new Location(player.getWorld(), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                }
                Team team = arena.getTeams().get(args[1]);
                if (team != null) {
                    EntityType entityType = EntityType.valueOf(args[2].toUpperCase());
                    if(!(TabArgsUtils.wrongEntities().contains(entityType))) {
                        if (team.isUpgradesSet()) DecoUtils.removeArmorStandAt(team.getTeamUpgrades().getLocation());
                        team.setTeamUpgrades(new TeamUpgrades(loc, entityType, team));
                        player.sendMessage(msg.getMsg(player, "setTeamUpgradesSuccess") + team.getTeamUpgrades().getLocation().getBlockX() + " " + team.getTeamUpgrades().getLocation().getBlockY() + " " + team.getTeamUpgrades().getLocation().getBlockZ());
                        DecoUtils.summonSetupArmorStand(team.getTeamUpgrades().getLocation(), team.getColor() + "Upgrades");
                    }else {
                        player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                    }
                } else {
                    player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                }
            }catch (Exception e) {
                player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            }
        }else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return TabArgsUtils.getTabEntities(player, args);
    }
}
