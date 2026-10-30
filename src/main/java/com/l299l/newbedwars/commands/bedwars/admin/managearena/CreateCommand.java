package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.extensions.PluginDetector;
import com.l299l.newbedwars.world.MainWorldCreator;
import com.l299l.newbedwars.world.ArenaWorldCreator;
import com.l299l.newbedwars.world.schematic.SchematicManager;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.gui.configuration.setup.guis.BasicConfigurationGUI;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CreateCommand extends BwCommand {

    private final ArenaWorldCreator worldCreator;

    public CreateCommand() {
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

    private static final java.util.regex.Pattern VALID_NAME = java.util.regex.Pattern.compile("[A-Za-z0-9_\\-]{1,64}");

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (args.length < 3) {
            usage(player);
            return;
        }
        String arenaName = args[2];
        if (!VALID_NAME.matcher(arenaName).matches()) {
            msg.send(player, "InvalidArenaName");
            return;
        }
        player.sendMessage(msg.getMsg(player, "StartArena").replace("/arenaname/", arenaName));
        if (ArenaRegistry.byName(arenaName) != null) {
            msg.send(player, "AlreadyCreated");
            return;
        }

        // an existing world (loaded or only on disk) must never be adopted: deleting the arena would delete it too
        if (Bukkit.getWorld(arenaName) != null || new java.io.File(Bukkit.getWorldContainer(), arenaName).exists()) {
            msg.send(player, "ArenaWorldExists");
            return;
        }

        String schematic = findFlag(args, "-sche");
        boolean noTeleport = hasFlag(args, "-n");

        if (schematic != null && !PluginDetector.isWorldEditPresent()) {
            msg.send(player, "WorldEditNotFound");
            return;
        }

        if (createArena(arenaName, schematic)) {
            player.sendMessage(msg.getMsg(player, "ArenaCreated").replace("/arenaname/", arenaName));
            if (noTeleport) return;
            try {
                IArena arena = ArenaRegistry.byName(arenaName);
                arena.getArenaWorld().save();
                player.setGameMode(GameMode.CREATIVE);
                player.teleport(new Location(arena.getArenaWorld(), 0, 100, 0));
                arena.getPlayersOnSetup().add(player.getUniqueId());
                player.openInventory(new BasicConfigurationGUI(NewBedwars.plugin.getGuiManager(), player).getInventory());
            } catch (Exception e) {
                msg.send(player, "ErrorOnCreate");
            }
        } else {
            if (schematic != null) {
                player.sendMessage(msg.getMsg(player, "SchematicNotFound").replace("/name/", schematic));
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
            if (PluginDetector.isWorldEditPresent()) opts.add("-sche");
            opts.add("-n");
            return opts;
        }

        if (args.length == 5 && args[3].equalsIgnoreCase("-sche") &&
                PluginDetector.isWorldEditPresent()) {
            return SchematicManager.listSchematics();
        }

        if (args.length == 6 && args[3].equalsIgnoreCase("-sche")) {
            return Collections.singletonList("-n");
        }

        if (args.length == 5 && args[3].equalsIgnoreCase("-n") && PluginDetector.isWorldEditPresent()) {
            return Collections.singletonList("-sche");
        }
        if (args.length == 6 && args[3].equalsIgnoreCase("-n") && args[4].equalsIgnoreCase("-sche") &&
                PluginDetector.isWorldEditPresent()) {
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
