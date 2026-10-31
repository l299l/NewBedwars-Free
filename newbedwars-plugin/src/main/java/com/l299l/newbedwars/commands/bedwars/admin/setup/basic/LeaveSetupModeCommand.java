package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.gui.configuration.setup.guis.ConfirmLeaveGUI;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class LeaveSetupModeCommand extends SubCommand {
    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Exit settings mode! Teleports to the spawn lobby!";
    }

    @Override
    public String getSyntax() {
        return "/bw leave (-all)";
    }

    @Override
    public String getExample() {
        return "/bw leave";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        player.openInventory(new ConfirmLeaveGUI(NewBedwars.plugin.getGuiManager(), player, (args.length == 2 && args[1].equalsIgnoreCase("-all"))).getInventory());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if(args.length == 2) {
            return Collections.singletonList("-all");
        }

        return null;
    }
}
