package com.l299l.newbedwars.commands.bedwars.admin.setup.automatic;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class StartAutomaticSetupCommand extends BwCommand {
    @Override
    public String getName() {
        return "startAutomaticSetup";
    }

    @Override
    public String getDescription() {
        return "Starts the automatic setup (not available yet).";
    }

    @Override
    public String getSyntax() {
        return "/bw startAutomaticSetup";
    }

    @Override
    public String getExample() {
        return "/bw startAutomaticSetup";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        msg.send(player, "AutomaticSetupUnavailable");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
