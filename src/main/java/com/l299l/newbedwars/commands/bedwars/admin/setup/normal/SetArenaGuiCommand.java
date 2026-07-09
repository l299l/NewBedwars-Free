package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetArenaGuiCommand extends SubCommand {

    @Override
    public String getName() {
        return "setGui";
    }

    @Override
    public String getDescription() {
        return "Set the shop or upgrade GUI used on this arena.";
    }

    @Override
    public String getSyntax() {
        return "/bw setGui <shop|upgrade> <guiId>";
    }

    @Override
    public String getExample() {
        return "/bw setGui shop exampleShop";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }
        String type = args[1].toLowerCase();
        String guiId = args[2];
        if (!type.equals("shop") && !type.equals("upgrade")) {
            player.sendMessage(ChatColor.RED + "Type must be 'shop' or 'upgrade'.");
            return;
        }
        if (NewBedwars.plugin.getGuiManager().getGui(guiId) == null) {
            player.sendMessage(ChatColor.RED + "GUI '" + guiId + "' not found.");
            return;
        }
        if (type.equals("shop")) {
            arena.setShopGuiId(guiId);
            player.sendMessage(ChatColor.GREEN + "Shop GUI set to '" + guiId + "'.");
        } else {
            arena.setUpgradeGuiId(guiId);
            player.sendMessage(ChatColor.GREEN + "Upgrade GUI set to '" + guiId + "'.");
        }
        new ArenaDataJson(arena).save();
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.of("shop", "upgrade");
        if (args.length == 3) return new ArrayList<>(NewBedwars.plugin.getGuiManager().getGuis().keySet());
        return List.of();
    }
}
