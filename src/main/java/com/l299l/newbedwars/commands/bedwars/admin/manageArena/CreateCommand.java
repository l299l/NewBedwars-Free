package com.l299l.newbedwars.commands.bedwars.admin.manageArena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.world.MainWorldCreator;
import com.l299l.newbedwars.world.WorldCreator;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.setup.guis.BasicConfigurationGUI;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CreateCommand extends SubCommand {

    private final Messages msg;
    private final WorldCreator worldCreator;

    public CreateCommand() {
        msg = NewBedwars.plugin.getMessages();
        worldCreator = new MainWorldCreator();
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Command to create new arens for bedwars! -n for don't teleport to arena, -sche for load map for schemat";
    }

    @Override
    public String getSyntax() {
        return "/bw arena create <name> (<-sche> <schematic>) (-n)";
    }

    @Override
    public String getExample() {
        return "/bw arena create castle -n";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (args.length > 2) {
            String startmessage =  msg.getMsg(player, "StartArena");
            player.sendMessage(startmessage.replaceAll("/arenaname/", args[2]));
            if (Arena.arenaByName.get(args[2]) != null) {
                msg.send(player, "AlreadyCreated");
                return;
            }

            if ((args.length == 5 || args.length == 6) && (args[3].equalsIgnoreCase("-sche") || args[4].equalsIgnoreCase("-sche"))) {
                try {
                } catch (Exception e) {
                    msg.send(player, "LoadingSchematicError");
                }
            } else if (createArena(args[2], null)) {
                try {
                    String message = msg.getMsg(player, "ArenaCreated");
                    player.sendMessage(message.replaceAll("/arenaname/", args[2]));
                    if((args.length == 4 || args.length == 6) && (args[3].equalsIgnoreCase("-n") || args[5].equalsIgnoreCase("-n"))) {
                        return;
                    }
                    IArena arena = Arena.arenaByName.get(args[2]);
                    Location loc = new Location(arena.getArenaWorld(), 0, 100, 0);
                    player.setGameMode(GameMode.CREATIVE);
                    player.teleport(loc);
                    arena.getPlayersOnSetup().add(player.getUniqueId());
                    player.openInventory(new BasicConfigurationGUI(NewBedwars.plugin.getGuiManager(), player).getInventory());
                } catch (Exception e) {
                    msg.send(player, "ErrorOnCreate");
                }
            } else {
                player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            }
        } else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 4) {
            ArrayList<String> arguments = new ArrayList<>();
            arguments.add("-sche");
            arguments.add("-n");
            return arguments;
        } else if (args.length == 5) {
            if(args[2].equalsIgnoreCase("-sche")) {
                return null; //return list of schemat
            }else {
                return Collections.singletonList("-sche");
            }
        } else if (args.length == 6) {
            if(args[3].equalsIgnoreCase("-sche")) {
                return null; //return list of schemat
            }else {
                return Collections.singletonList("-n");
            }
        }
        return null;
    }

    private boolean createArena(String arenaName, String schematic) {
        boolean a;
        try {
            if (schematic != null) {
                a = worldCreator.createWorldFromSchematic(arenaName, schematic);
                new Arena(arenaName, Objects.requireNonNull(Bukkit.getWorld(arenaName)));
                return a;
            } else {
                a = worldCreator.createWorld(arenaName);
                new Arena(arenaName, Objects.requireNonNull(Bukkit.getWorld(arenaName)));
                return a;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
