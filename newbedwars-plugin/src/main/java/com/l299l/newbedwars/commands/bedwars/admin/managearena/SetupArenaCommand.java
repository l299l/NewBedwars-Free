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
        if(args.length > 2) {
            try {
                IArena arena = ArenaRegistry.byName(args[2]);
                if(arena.getSetupMode() != Setup.READY) {
                    Location loc = new Location(arena.getArenaWorld(), 0, 100, 0);
                    if (args.length > 3) {
                        if (args[3].equalsIgnoreCase("-l") && args.length >= 5) {
                            for (int i = 0; i < (args.length - 4); i++) {
                                Player p = Bukkit.getPlayer(args[i + 4]);
                                {
                                    assert p != null;
                                    if (!p.hasPermission("newbedwars.bw.bypass")) {
                                        p.teleport(loc);
                                        arena.getPlayersOnSetup().add(p.getUniqueId());
                                        p.setGameMode(GameMode.CREATIVE);
                                        msg.send(p, "AddedToSetupMode");
                                        if(arena.getSetupMode() != Setup.AUTOMATIC_SETUP) {
                                            msg.sendConf(p, Setup.NORMAL_SETUP);
                                        }else {
                                            msg.sendConf(p, Setup.AUTOMATIC_SETUP);
                                        }
                                    } else {
                                        player.sendMessage(msg.getMsg(player, "AdminHaveBypass").replace("/admin/", p.getName()));
                                    }
                                }
                            }
                        } else {
                            usage(player);
                        }
                    }
                    player.teleport(loc);
                    arena.getPlayersOnSetup().add(player.getUniqueId());
                    if (arena.getSetupMode() == Setup.NO_SETUP || arena.getSetupMode() == null) {
                        player.openInventory(new BasicConfigurationGUI(NewBedwars.plugin.getGuiManager(), player).getInventory());
                    }else {
                        if(arena.getSetupMode() != Setup.AUTOMATIC_SETUP) {
                            msg.sendConf(player, Setup.NORMAL_SETUP);
                        }else {
                            msg.sendConf(player, Setup.AUTOMATIC_SETUP);
                        }
                    }
                    player.setGameMode(GameMode.CREATIVE);
                }else {
                    msg.send(player, "ArenaReady");
                }
            }catch (Exception e) {
                msg.send(player, "ArenaNotExists");
            }
        }
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
