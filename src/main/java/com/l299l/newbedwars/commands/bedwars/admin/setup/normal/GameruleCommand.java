package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class GameruleCommand extends SubCommand {
    private static final List<String> GAMERULE_NAMES = Arrays.asList(
            "RandomTeams", "AllowParties", "AllowSpectators", "AllowTeamChat",
            "AllowGlobalChat", "AllowPrivateChat", "AllowTeamDamage", "MakeSwordsPermanent"
    );
    private final Messages msg;

    public GameruleCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "gamerule";
    }

    @Override
    public String getDescription() {
        return "Set a gamerule for this arena.";
    }

    @Override
    public String getSyntax() {
        return "/bw gamerule <name> <true|false>";
    }

    @Override
    public String getExample() {
        return "/bw gamerule AllowParties true";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length != 3) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        String name = args[1];
        String valueStr = args[2].toLowerCase();
        if (!valueStr.equals("true") && !valueStr.equals("false")) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        boolean nameValid = GAMERULE_NAMES.stream().anyMatch(g -> g.equalsIgnoreCase(name));
        if (!nameValid) {
            player.sendMessage(msg.getMsg(player, "GameruleInvalid"));
            return;
        }
        String canonicalName = GAMERULE_NAMES.stream().filter(g -> g.equalsIgnoreCase(name)).findFirst().get();
        boolean value = Boolean.parseBoolean(valueStr);
        arena.getGamerules().setGamerule(canonicalName, value);
        player.sendMessage(msg.getMsg(player, "GameruleSet")
                .replace("/gamerule/", name)
                .replace("/value/", String.valueOf(value)));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return GAMERULE_NAMES;
        if (args.length == 3) return Arrays.asList("true", "false");
        return List.of();
    }
}
