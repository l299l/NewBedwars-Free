package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.*;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.utils.DecoUtils;
import com.l299l.newbedwars.utils.TabArgsUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetGeneratorCommand extends SubCommand {

    private final Messages msg;

    public SetGeneratorCommand() {
        this.msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "setGenerator";
    }

    @Override
    public String getDescription() {
        return "Set the diamond/emerald/team generator on the arena.";
    }

    @Override
    public String getSyntax() {
        return "/bw setGenerator <generatorType> (<generatorTeamName>) (<x> <y> <z>)";
    }

    @Override
    public String getExample() {
        return "/bw setGenerator emerald 10 6 -15";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        try {
            GeneratorType type = GeneratorType.valueOf(args[1].toUpperCase());
            if (type == GeneratorType.DIAMOND || type == GeneratorType.EMERALD) {
                if (args.length == 2) {
                    arena.addGenerator(createGenerator(type, player.getLocation()));
                    player.sendMessage(msg.getMsg(player, "setGeneratorSuccess") + type.name() + " " + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
                    DecoUtils.summonSetupArmorStand(player.getLocation(), (type == GeneratorType.EMERALD ? ChatColor.GREEN : ChatColor.BLUE)+ "Generator");
                    return;
                }
                if (args.length < 5) {
                    player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                    return;
                }
                try {
                    int x = Integer.parseInt(args[2]);
                    int y = Integer.parseInt(args[3]);
                    int z = Integer.parseInt(args[4]);
                    arena.addGenerator(createGenerator(type, new Location(player.getWorld(), x, y, z)));
                    player.sendMessage(msg.getMsg(player, "setGeneratorSuccess") + type.name() + " " + x + " " + y + " " + z);
                    DecoUtils.summonSetupArmorStand(new Location(player.getWorld(), x, y, z), (type == GeneratorType.EMERALD ? ChatColor.GREEN : ChatColor.BLUE)+ "Generator");
                } catch (Exception e) {
                    player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                }
            } else if (type == GeneratorType.BASIC) {
                if (args.length == 3) {
                    Team team = arena.getTeams().get(args[2]);
                    if (team == null) {
                        player.sendMessage(msg.getMsg(player, "TeamNotFound"));
                        return;
                    }
                    if (team.getGenerator() != null) {
                        DecoUtils.removeArmorStandAt(team.getGenerator().getLocation());
                    }
                    team.setGenerator(createGenerator(type, player.getLocation()));
                    player.sendMessage(msg.getMsg(player, "setGeneratorSuccess") + type.name() + " " + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
                    DecoUtils.summonSetupArmorStand(player.getLocation(), team.getColor() + "Generator");
                    return;
                }
                if (args.length == 6) {
                    try {
                        Team team = arena.getTeams().get(args[2]);
                        if (team == null) {
                            player.sendMessage(msg.getMsg(player, "TeamNotFound"));
                            return;
                        }
                        if (team.getGenerator() != null) {
                            DecoUtils.removeArmorStandAt(team.getGenerator().getLocation());
                        }
                        int x = Integer.parseInt(args[3]);
                        int y = Integer.parseInt(args[4]);
                        int z = Integer.parseInt(args[5]);
                        team.setGenerator(createGenerator(type, new Location(player.getWorld(), x, y, z)));
                        player.sendMessage(msg.getMsg(player, "setGeneratorSuccess") + type.name() + " " + x + " " + y + " " + z);
                        DecoUtils.summonSetupArmorStand(team.getGenerator().getLocation(), team.getColor() + "Generator");
                    } catch (Exception e) {
                        player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                    }
                }else {
                    player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                }
            }else {
                if (arena.getSetupMode() == Setup.ADVANCED_SETUP) {
                }else {
                    player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
                }
            }
        } catch(Exception e){
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        IArena arena = Arena.arenaByWorld.get(player.getWorld());
        if (args.length == 2) {
            if (arena.getSetupMode() == Setup.ADVANCED_SETUP) {
                return new ArrayList<>(Arrays.asList("DIAMOND", "EMERALD", "BASIC", "OTHER"));
            }else {
                return new ArrayList<>(Arrays.asList("DIAMOND", "EMERALD", "BASIC"));
            }
        }
        if (args.length > 2) {
            if (args[1].equalsIgnoreCase("DIAMOND") || args[1].equalsIgnoreCase("EMERALD")) {
                return TabArgsUtils.getTabCords(player, args, 3);
            } else if (args[1].equalsIgnoreCase("BASIC")) {
                return TabArgsUtils.getTabCordsWithTeam(player, args, 3);
            }else if (arena.getSetupMode() == Setup.ADVANCED_SETUP) {
                return null;
            }
        }
        return null;
    }

    private Generator createGenerator(GeneratorType type, Location location) {
        return switch (type) {
            case DIAMOND -> new DiamondGenerator(location);
            case EMERALD -> new EmeraldGenerator(location);
            case BASIC -> new BasicGenerator(location);
            default -> null;
        };
    }

}
