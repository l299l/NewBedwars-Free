package com.l299l.newbedwars.commands.bedwars.admin.setup.automatic;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class StartAutomaticSetupCommand extends SubCommand {
    @Override
    public String getName() {
        return "startAutomaticSetup";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getSyntax() {
        return null;
    }

    @Override
    public String getExample() {
        return null;
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {

    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
