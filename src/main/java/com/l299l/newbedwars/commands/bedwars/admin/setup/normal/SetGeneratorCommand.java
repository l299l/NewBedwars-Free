package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.*;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.arena.setup.DecoUtils;
import com.l299l.newbedwars.commands.bedwars.admin.setup.TabArgsUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetGeneratorCommand extends BwCommand {



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
            usage(player);
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
                    usage(player);
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
                    usage(player);
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
                        usage(player);
                    }
                }else {
                    usage(player);
                }
            }else {
                if (arena.getSetupMode() == Setup.ADVANCED_SETUP) {
                }else {
                    usage(player);
                }
            }
        } catch(Exception e){
            usage(player);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        if (arena == null) return new ArrayList<>();
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
