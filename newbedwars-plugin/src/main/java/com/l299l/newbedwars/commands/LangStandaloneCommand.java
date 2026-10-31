package com.l299l.newbedwars.commands;

import com.l299l.newbedwars.commands.bedwars.main.LangCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LangStandaloneCommand implements CommandExecutor, TabCompleter {
    private final LangCommand delegate = new LangCommand();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        String[] shifted = new String[args.length + 1];
        shifted[0] = "lang";
        System.arraycopy(args, 0, shifted, 1, args.length);
        delegate.perform(player, shifted, null);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player)) return null;
        String[] shifted = new String[args.length + 1];
        shifted[0] = "lang";
        System.arraycopy(args, 0, shifted, 1, args.length);
        return delegate.getSubcommandArguments(player, shifted);
    }
}
