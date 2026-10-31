package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.gui.configuration.setup.guis.BasicConfigurationGUI;
import org.bukkit.entity.Player;

import java.util.List;

public class SetupModeCommand extends SubCommand {
    @Override
    public String getName() {
        return "setupMode";
    }

    @Override
    public String getDescription() {
        return "Open the setup mode selection GUI";
    }

    @Override
    public String getSyntax() {
        return "/bw setupMode";
    }

    @Override
    public String getExample() {
        return "/bw setupMode";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        player.openInventory(new BasicConfigurationGUI(NewBedwars.plugin.getGuiManager(), player).getInventory());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
