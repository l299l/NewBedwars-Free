package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SetArenaGUICommand extends BwCommand {

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
            usage(player);
            return;
        }
        String type = args[1].toLowerCase();
        String guiId = args[2];
        if (!type.equals("shop") && !type.equals("upgrade")) {
            msg.send(player, "SetGuiTypeInvalid");
            return;
        }
        if (NewBedwars.plugin.getGuiManager().getGui(guiId) == null) {
            msg.send(player, "SetGuiNotFound", Map.of("/gui/", guiId));
            return;
        }
        if (type.equals("shop")) {
            arena.setShopGuiId(guiId);
            msg.send(player, "SetGuiShopSet", Map.of("/gui/", guiId));
        } else {
            arena.setUpgradeGuiId(guiId);
            msg.send(player, "SetGuiUpgradeSet", Map.of("/gui/", guiId));
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
