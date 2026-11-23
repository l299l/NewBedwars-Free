package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.tablist.TablistConfig;
import com.l299l.newbedwars.tablist.TablistLayout;
import com.l299l.newbedwars.tablist.TablistManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class SetTablistCommand extends BwCommand {

    @Override
    public String getName() {
        return "setTablist";
    }

    @Override
    public String getDescription() {
        return "Set which tab list layout (tablists.yml) this arena uses.";
    }

    @Override
    public String getSyntax() {
        return "/bw setTablist <layout>";
    }

    @Override
    public String getExample() {
        return "/bw setTablist Default";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length != 2) {
            usage(player);
            return;
        }
        TablistManager manager = NewBedwars.plugin.getTablistManager();
        TablistConfig config = manager == null ? null : manager.getConfig();
        TablistLayout layout = config == null ? null : config.find(args[1]);
        if (layout == null) {
            msg.send(player, "TablistInvalid", Map.of(
                    "/id/", args[1],
                    "/available/", String.join(", ", available())));
            return;
        }
        arena.setTablistId(layout.id());
        new ArenaDataJson(arena).save();
        msg.send(player, "TablistSet", Map.of("/id/", layout.id()));
    }

    private static TreeSet<String> available() {
        TablistManager manager = NewBedwars.plugin.getTablistManager();
        return manager == null ? new TreeSet<>() : new TreeSet<>(manager.getConfig().layouts().keySet());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.copyOf(available());
        return List.of();
    }
}
