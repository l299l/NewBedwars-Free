package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetupHelpCommand extends SubCommand {
    private final ArrayList<SubCommand> subCommands;
    private final Messages msg;

    public SetupHelpCommand(ArrayList<SubCommand> subCommands) {
        this.subCommands = subCommands;
        msg = NewBedwars.plugin.getMessages();
        subCommands.add(this);
    }

    @Override
    public String getName() {
        return "setupHelp";
    }

    @Override
    public String getDescription() {
        return "Basic setup help command";
    }

    @Override
    public String getSyntax() {
        return "/bw setupHelp (<page>) (<command>)";
    }

    @Override
    public String getExample() {
        return "/bw setupHelp specialGamerule";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length == 1) {
            printHelp(player, 1);
        }else if(args.length == 2) {
            try {
                int page = Integer.parseInt(args[1]);
                printHelp(player, page);
            } catch (NumberFormatException e) {
                printCommandHelp(player, args[1]);
            }
        }else {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        int pages = (int) Math.ceil(subCommands.size() / 10.0);
        if (args.length == 2) {
            List<String> ans = new ArrayList<>();
            for (int i = 1; i <= pages; i++) {
                ans.add(String.valueOf(i));
            }
            for (SubCommand subCommand : subCommands) {
                ans.add(subCommand.getName());
            }
            return ans;
        }
        return null;
    }

    private void printHelp(Player p, Integer page) {
        int pages = (int) Math.ceil(subCommands.size() / 10.0);
        if (page > pages) {
            msg.send(p, "PageDoesntExists");
            return;
        }
        p.sendMessage(ChatColor.AQUA + "=-=-=-=-=-=-=-SetupHelp-=-=-=-=-=-=-=");
        for (int i = (page * 10) - 10; i < (page * 10); i++) {
            if (subCommands.size() <= i) {
                break;
            }
            p.sendMessage(ChatColor.YELLOW + subCommands.get(i).getSyntax());
        }
        p.sendMessage(ChatColor.AQUA + "=-=-=-=-=-=-=-Page  " + page + "/" + pages + "-=-=-=-=-=-=-=");
    }

    private void printCommandHelp(Player p, String cmd) {
        SubCommand command = null;
        for (SubCommand subCommand : subCommands) {
            if (subCommand.getName().equalsIgnoreCase(cmd)) {
                command = subCommand;
                break;
            }
        }
        if (command == null) {
            msg.send(p, "CommandDoesntExists");
        }
        p.sendMessage(ChatColor.AQUA + "=-=-=-=-=-=-=-SetupHelp-=-=-=-=-=-=-=");
        assert command != null;
        p.sendMessage(ChatColor.GREEN + "Command Name: " + ChatColor.YELLOW + command.getName() + "\n");
        p.sendMessage(command.getDescription() + "\n\n");
        p.sendMessage(ChatColor.GREEN + "Command Syntax:\n" + ChatColor.YELLOW + command.getSyntax());
        p.sendMessage(ChatColor.GREEN + "Command Example:\n" + ChatColor.YELLOW + command.getExample());
        p.sendMessage(ChatColor.AQUA + "=-=-=-=-=-=-=-SetupHelp-=-=-=-=-=-=-=");
    }
}
