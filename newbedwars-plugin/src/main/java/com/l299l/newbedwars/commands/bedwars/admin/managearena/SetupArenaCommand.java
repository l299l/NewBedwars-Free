package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.gui.configuration.setup.guis.BasicConfigurationGUI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SetupArenaCommand extends BwCommand {
    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getDescription() {
        return "It allows you to enter the setup mode of a given arena.";
    }

    @Override
    public String getExample() {
        return "/bw arena setup castle -l toster328 Rabarbar14";
    }

    @Override
    public String getSyntax() {
        return "/bw arena setup <arenaName> (-l <listOfAdmins>)";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (args.length <= 2) return;
        IArena arena = ArenaRegistry.byName(args[2]);
        if (arena == null) {
            msg.send(player, "ArenaNotExists");
            return;
        }
        if (arena.getSetupMode() == Setup.READY) {
            msg.send(player, "ArenaReady");
            return;
        }
        Location loc = new Location(arena.getArenaWorld(), 0, 100, 0);
        if (args.length > 3) {
            if (args[3].equalsIgnoreCase("-l") && args.length >= 5) {
                for (int i = 4; i < args.length; i++) {
                    addAdmin(player, arena, args[i], loc);
                }
            } else {
                usage(player);
            }
        }
        player.teleport(loc);
        arena.getPlayersOnSetup().add(player.getUniqueId());
        if (arena.getSetupMode() == Setup.NO_SETUP || arena.getSetupMode() == null) {
            player.openInventory(new BasicConfigurationGUI(NewBedwars.plugin.getGuiManager(), player).getInventory());
        } else {
            msg.sendConf(player, setupMessageMode(arena));
        }
        player.setGameMode(GameMode.CREATIVE);
    }

    private void addAdmin(Player player, IArena arena, String name, Location loc) {
        Player p = Bukkit.getPlayer(name);
        if (p == null) {
            player.sendMessage(msg.getMsg(player, "PlayerNotOnline").replace("/player/", name));
            return;
        }
        if (p.hasPermission("newbedwars.bw.bypass")) {
            player.sendMessage(msg.getMsg(player, "AdminHaveBypass").replace("/admin/", p.getName()));
            return;
        }
        p.teleport(loc);
        arena.getPlayersOnSetup().add(p.getUniqueId());
        p.setGameMode(GameMode.CREATIVE);
        msg.send(p, "AddedToSetupMode");
        msg.sendConf(p, setupMessageMode(arena));
    }

    private Setup setupMessageMode(IArena arena) {
        return arena.getSetupMode() != Setup.AUTOMATIC_SETUP ? Setup.NORMAL_SETUP : Setup.AUTOMATIC_SETUP;
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if(args.length == 3) {
            Set<String> arenaNames = ArenaRegistry.names();
            return new ArrayList<>(arenaNames);
        }else if(args.length == 4) {
            return Collections.singletonList("-l");
        }
        return null;
    }
}
