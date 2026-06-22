package com.l299l.newbedwars.commands.bedwars.admin.manageArena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.world.MainWorldCreator;
import com.l299l.newbedwars.world.WorldCreator;
import com.l299l.newbedwars.world.schematic.SchematicManager;
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
        if (args.length < 3) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        String arenaName = args[2];
        player.sendMessage(msg.getMsg(player, "StartArena").replaceAll("/arenaname/", arenaName));
        if (Arena.arenaByName.get(arenaName) != null) {
            msg.send(player, "AlreadyCreated");
            return;
        }

        String schematic = findFlag(args, "-sche");
        boolean noTeleport = hasFlag(args, "-n");

        if (schematic != null && !SchematicManager.isWorldEditPresent()) {
            msg.send(player, "WorldEditNotFound");
            return;
        }

        if (createArena(arenaName, schematic)) {
            player.sendMessage(msg.getMsg(player, "ArenaCreated").replaceAll("/arenaname/", arenaName));
            if (noTeleport) return;
            try {
                IArena arena = Arena.arenaByName.get(arenaName);
                player.setGameMode(GameMode.CREATIVE);
                player.teleport(new Location(arena.getArenaWorld(), 0, 100, 0));
                arena.getPlayersOnSetup().add(player.getUniqueId());
                player.openInventory(new BasicConfigurationGUI(NewBedwars.plugin.getGuiManager(), player).getInventory());
            } catch (Exception e) {
                msg.send(player, "ErrorOnCreate");
            }
        } else {
            if (schematic != null) {
                player.sendMessage(msg.getMsg(player, "SchematicNotFound").replaceAll("/name/", schematic));
            } else {
                msg.send(player, "ErrorOnCreate");
            }
        }
    }

    private String findFlag(String[] args, String flag) {
        for (int i = 3; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(flag)) return args[i + 1];
        }
        return null;
    }

    private boolean hasFlag(String[] args, String flag) {
        for (int i = 3; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(flag)) return true;
        }
        return false;
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 4) {
            ArrayList<String> opts = new ArrayList<>();
            opts.add("-sche");
            opts.add("-n");
            return opts;
        }
        // After "-sche", suggest available schematic names
        if (args.length == 5 && args[3].equalsIgnoreCase("-sche")) {
            return SchematicManager.listSchematics();
        }
        // After "-sche <name>", offer "-n"
        if (args.length == 6 && args[3].equalsIgnoreCase("-sche")) {
            return Collections.singletonList("-n");
        }
        // After "-n", offer "-sche"
        if (args.length == 5 && args[3].equalsIgnoreCase("-n")) {
            return Collections.singletonList("-sche");
        }
        if (args.length == 6 && args[3].equalsIgnoreCase("-n") && args[4].equalsIgnoreCase("-sche")) {
            return SchematicManager.listSchematics();
        }
        return null;
    }

    private boolean createArena(String arenaName, String schematic) {
        try {
            boolean ok = schematic != null
                    ? worldCreator.createWorldFromSchematic(arenaName, schematic)
                    : worldCreator.createWorld(arenaName);
            if (!ok) return false;
            new Arena(arenaName, Objects.requireNonNull(Bukkit.getWorld(arenaName)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
